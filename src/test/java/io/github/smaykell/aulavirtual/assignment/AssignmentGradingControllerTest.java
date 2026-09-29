package io.github.smaykell.aulavirtual.assignment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.assignment.dto.GradeData;
import io.github.smaykell.aulavirtual.assignment.dto.HandBackRequest;
import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
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

@WebMvcTest(AssignmentGradingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class AssignmentGradingControllerTest {

    private static final UUID ASSIGNMENT = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AssignmentGradingService gradingService;

    @Test
    void the_teacher_grades_a_student_of_the_task() throws Exception {
        when(gradingService.grade(eq("ana"), eq(ASSIGNMENT), eq(STUDENT), any(GradeData.class)))
                .thenReturn(new GradeResponse(UUID.randomUUID(), GradeSource.ASSIGNMENT,
                        ASSIGNMENT, STUDENT, UUID.randomUUID(), new BigDecimal("18.00"),
                        new BigDecimal("20.00"), "Buen trabajo", Instant.EPOCH, null));

        mockMvc.perform(put("/assignments/{id}/grades/{studentId}", ASSIGNMENT, STUDENT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\": 18.00, \"feedback\": \"Buen trabajo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(18.00))
                .andExpect(jsonPath("$.maxScore").value(20.00));
    }

    @Test
    void a_student_does_not_grade() throws Exception {
        mockMvc.perform(put("/assignments/{id}/grades/{studentId}", ASSIGNMENT, STUDENT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\": 20.00}"))
                .andExpect(status().isForbidden());

        verify(gradingService, never()).grade(any(), any(), any(), any());
    }

    @Test
    void a_negative_score_does_not_reach_the_service() throws Exception {
        mockMvc.perform(put("/assignments/{id}/grades/{studentId}", ASSIGNMENT, STUDENT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("score"));

        verify(gradingService, never()).grade(any(), any(), any(), any());
    }

    @Test
    void the_teacher_hands_back_the_grades_of_several_students_at_once() throws Exception {
        when(gradingService.handBack(eq("ana"), eq(ASSIGNMENT), any(HandBackRequest.class)))
                .thenReturn(List.of());

        mockMvc.perform(post("/assignments/{id}/grades/$return", ASSIGNMENT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\": [\"" + STUDENT + "\"]}"))
                .andExpect(status().isOk());

        verify(gradingService).handBack("ana", ASSIGNMENT, new HandBackRequest(List.of(STUDENT)));
    }

    @Test
    void handing_back_to_nobody_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/assignments/{id}/grades/$return", ASSIGNMENT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("studentIds"));

        verify(gradingService, never()).handBack(any(), any(), any());
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
