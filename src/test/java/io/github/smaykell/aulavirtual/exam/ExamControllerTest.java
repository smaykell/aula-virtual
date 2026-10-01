package io.github.smaykell.aulavirtual.exam;

import static io.github.smaykell.aulavirtual.exam.ExamFixtures.UNIT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.exam.dto.ExamData;
import io.github.smaykell.aulavirtual.exam.dto.ExamResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
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

@WebMvcTest(ExamController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class ExamControllerTest {

    private static final String EXAM = """
            {"title": "Primer parcial", "opensAt": "2026-10-15T13:00:00Z",
             "closesAt": "2026-10-15T15:00:00Z", "timeLimitMinutes": 45, "maxAttempts": 1,
             "shuffleQuestions": false, "shuffleOptions": false, "showsAnswers": true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ExamService examService;

    @Test
    void a_teacher_schedules_an_exam_and_gets_a_201() throws Exception {
        when(examService.create(eq("ana"), eq(UNIT), any(ExamData.class)))
                .thenReturn(ExamResponse.from(ExamFixtures.exam(), 0));

        mockMvc.perform(post("/units/{unitId}/exams", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EXAM))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Primer parcial"))
                .andExpect(jsonPath("$.closesAt").value("2026-10-15T15:00:00Z"));
    }

    @Test
    void a_student_reads_the_exams_of_its_unit() throws Exception {
        when(examService.list("ana", UNIT))
                .thenReturn(List.of(ExamResponse.from(ExamFixtures.exam(), 10)));

        mockMvc.perform(get("/units/{unitId}/exams", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionCount").value(10));
    }

    @Test
    void a_student_does_not_schedule_exams() throws Exception {
        mockMvc.perform(post("/units/{unitId}/exams", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EXAM))
                .andExpect(status().isForbidden());

        verify(examService, never()).create(any(), any(), any());
    }

    @Test
    void a_student_does_not_read_the_questions_of_an_exam() throws Exception {
        mockMvc.perform(get("/exams/{id}/questions", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isForbidden());

        verify(examService, never()).questions(any(), any());
    }

    @Test
    void an_exam_without_a_closing_time_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/units/{unitId}/exams", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EXAM.replace("\"closesAt\": \"2026-10-15T15:00:00Z\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("closesAt"));

        verify(examService, never()).create(any(), any(), any());
    }

    @Test
    void a_third_attempt_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/units/{unitId}/exams", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EXAM.replace("\"maxAttempts\": 1", "\"maxAttempts\": 3")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("maxAttempts"));

        verify(examService, never()).create(any(), any(), any());
    }

    @Test
    void a_question_without_points_does_not_reach_the_service() throws Exception {
        mockMvc.perform(put("/exams/{id}/questions", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questions\": [{\"questionId\": \"%s\"}]}"
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verify(examService, never()).replaceQuestions(any(), any(), any());
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
