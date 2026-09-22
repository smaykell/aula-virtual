package io.github.smaykell.aulavirtual.assignment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.assignment.dto.GradeData;
import io.github.smaykell.aulavirtual.assignment.dto.GradeResponse;
import io.github.smaykell.aulavirtual.assignment.dto.SubmissionData;
import io.github.smaykell.aulavirtual.assignment.dto.SubmissionResponse;
import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubmissionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class SubmissionControllerTest {

    private static final UUID ASSIGNMENT = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private SubmissionService submissionService;

    @Test
    void without_a_token_handing_in_answers_401() throws Exception {
        mockMvc.perform(post("/assignments/{id}/$submit", ASSIGNMENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionBody()))
                .andExpect(status().isUnauthorized());

        verify(submissionService, never()).submit(any(), any(), any());
    }

    @Test
    void the_student_hands_in_the_task() throws Exception {
        when(submissionService.submit(eq("ana"), eq(ASSIGNMENT), any(SubmissionData.class)))
                .thenReturn(aSubmission(SubmissionStatus.SUBMITTED, null));

        mockMvc.perform(post("/assignments/{id}/$submit", ASSIGNMENT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.text").value("Mi respuesta"));
    }

    @Test
    void a_teacher_does_not_hand_in_tasks() throws Exception {
        mockMvc.perform(post("/assignments/{id}/$submit", ASSIGNMENT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GEN_ACCESS_DENIED"));

        verify(submissionService, never()).submit(any(), any(), any());
    }

    @Test
    void the_status_filter_of_the_submissions_reaches_the_service() throws Exception {
        when(submissionService.list(eq("ana"), eq(ASSIGNMENT), eq(SubmissionStatus.LATE), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/assignments/{id}/submissions", ASSIGNMENT)
                        .param("status", "LATE")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isOk());

        verify(submissionService).list(eq("ana"), eq(ASSIGNMENT), eq(SubmissionStatus.LATE),
                any());
    }

    @Test
    void the_teacher_grades_a_submission_and_the_grade_travels_with_it() throws Exception {
        UUID submissionId = UUID.randomUUID();
        when(submissionService.grade(eq("ana"), eq(submissionId), any(GradeData.class)))
                .thenReturn(aSubmission(SubmissionStatus.GRADED, new BigDecimal("18.00")));

        mockMvc.perform(post("/submissions/{id}/$grade", submissionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\": 18.00, \"feedback\": \"Buen trabajo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRADED"))
                .andExpect(jsonPath("$.grade.score").value(18.00));
    }

    @Test
    void a_student_does_not_grade_its_own_submission() throws Exception {
        mockMvc.perform(post("/submissions/{id}/$grade", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\": 20.00}"))
                .andExpect(status().isForbidden());

        verify(submissionService, never()).grade(any(), any(), any());
    }

    @Test
    void a_negative_score_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/submissions/{id}/$grade", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("score"));

        verify(submissionService, never()).grade(any(), any(), any());
    }

    private SubmissionResponse aSubmission(SubmissionStatus status, BigDecimal score) {
        UUID id = UUID.randomUUID();
        GradeResponse grade = score == null ? null : new GradeResponse(UUID.randomUUID(),
                GradeSource.ASSIGNMENT, id, UUID.randomUUID(), UUID.randomUUID(), score,
                "Buen trabajo", Instant.EPOCH);

        return new SubmissionResponse(id, ASSIGNMENT,
                new StudentSummary(UUID.randomUUID(), "Ana Maria", "Quispe Rojas", "Hospital Regional", true),
                null, "Mi respuesta", Instant.EPOCH, status, grade);
    }

    private String submissionBody() {
        return """
                {"text": "Mi respuesta"}
                """;
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
