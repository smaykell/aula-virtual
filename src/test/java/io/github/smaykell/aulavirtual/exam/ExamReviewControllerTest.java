package io.github.smaykell.aulavirtual.exam;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@WebMvcTest(ExamReviewController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class ExamReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ExamReviewService reviewService;

    @Test
    void a_student_reads_its_own_results() throws Exception {
        UUID exam = UUID.randomUUID();

        mockMvc.perform(get("/exams/{id}/results", exam)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isOk());

        verify(reviewService).results("ana", exam);
    }

    @Test
    void a_student_does_not_score_answers() throws Exception {
        mockMvc.perform(put("/attempts/{id}/answers/{questionId}/score", UUID.randomUUID(),
                        UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\": 20}"))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).score(any(), any(), any(), any());
    }

    @Test
    void a_negative_score_does_not_reach_the_service() throws Exception {
        mockMvc.perform(put("/attempts/{id}/answers/{questionId}/score", UUID.randomUUID(),
                        UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("points"));

        verify(reviewService, never()).score(any(), any(), any(), any());
    }

    @Test
    void handing_back_without_students_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/exams/{id}/grades/$return", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\": []}"))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).handBack(any(), any(), any());
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
