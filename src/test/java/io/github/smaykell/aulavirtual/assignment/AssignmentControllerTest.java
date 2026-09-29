package io.github.smaykell.aulavirtual.assignment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.assignment.dto.AssignmentData;
import io.github.smaykell.aulavirtual.assignment.dto.AssignmentResponse;
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

@WebMvcTest(AssignmentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class AssignmentControllerTest {

    private static final UUID UNIT = AssignmentFixtures.UNIT;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AssignmentService assignmentService;

    @Test
    void without_a_token_the_tasks_of_a_unit_answer_401() throws Exception {
        mockMvc.perform(get("/units/{unitId}/assignments", UNIT))
                .andExpect(status().isUnauthorized());

        verify(assignmentService, never()).list(any(), any());
    }

    @Test
    void a_teacher_publishes_a_task_and_gets_a_201() throws Exception {
        when(assignmentService.create(eq("ana"), eq(UNIT), any(AssignmentData.class)))
                .thenReturn(anAssignment());

        mockMvc.perform(post("/units/{unitId}/assignments", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentBody("20.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Practica 1"))
                .andExpect(jsonPath("$.maxScore").value(20.00))
                .andExpect(jsonPath("$.allowsLate").value(false));
    }

    @Test
    void a_student_does_not_publish_tasks() throws Exception {
        mockMvc.perform(post("/units/{unitId}/assignments", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentBody("20.00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GEN_ACCESS_DENIED"));

        verify(assignmentService, never()).create(any(), any(), any());
    }

    @Test
    void a_student_reads_the_tasks_of_its_unit() throws Exception {
        when(assignmentService.list("ana", UNIT)).thenReturn(List.of(anAssignment()));

        mockMvc.perform(get("/units/{unitId}/assignments", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Practica 1"));
    }

    @Test
    void a_task_with_a_score_of_zero_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/units/{unitId}/assignments", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentBody("0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("maxScore"));

        verify(assignmentService, never()).create(any(), any(), any());
    }

    @Test
    void a_task_without_a_deadline_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/units/{unitId}/assignments", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Practica 1\", \"maxScore\": 20,"
                                + " \"allowsLate\": false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("dueAt"));

        verify(assignmentService, never()).create(any(), any(), any());
    }

    @Test
    void a_student_does_not_delete_a_task() throws Exception {
        mockMvc.perform(delete("/assignments/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isForbidden());

        verify(assignmentService, never()).delete(any(), any());
    }

    @Test
    void the_teacher_deletes_a_task_and_gets_a_204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/assignments/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isNoContent());

        verify(assignmentService).delete("ana", id);
    }

    private AssignmentResponse anAssignment() {
        return new AssignmentResponse(UUID.randomUUID(), UNIT, "Practica 1",
                "Resuelve los ejercicios", Instant.parse("2026-04-20T23:59:00Z"),
                new BigDecimal("20.00"), false, null, null, Instant.EPOCH);
    }

    private String assignmentBody(String maxScore) {
        return """
                {"title": "Practica 1", "instructions": "Resuelve los ejercicios",
                 "dueAt": "2026-04-20T23:59:00Z", "maxScore": %s, "allowsLate": false}
                """.formatted(maxScore);
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
