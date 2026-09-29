package io.github.smaykell.aulavirtual.gradebook;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.gradebook.dto.FinalGrade;
import io.github.smaykell.aulavirtual.gradebook.dto.GradebookResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradebookRow;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.math.BigDecimal;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GradebookController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class GradebookControllerTest {

    private static final UUID COURSE = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private GradebookService gradebookService;

    @Test
    void without_a_token_the_gradebook_answers_401() throws Exception {
        mockMvc.perform(get("/courses/{courseId}/gradebook", COURSE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void the_student_reads_the_gradebook_and_the_service_decides_which_rows() throws Exception {
        when(gradebookService.of("ana", COURSE)).thenReturn(new GradebookResponse(
                new GradingSchemeResponse(GradingMethod.TOTAL_POINTS, new BigDecimal("13.00"),
                        List.of()),
                List.of(),
                List.of(new GradebookRow(
                        new StudentSummary(UUID.randomUUID(), "Ana", "Quispe Rojas",
                                "Hospital Regional", true),
                        List.of(), List.of(),
                        FinalGrade.of(new BigDecimal("12.5"), new BigDecimal("13.00"))))));

        mockMvc.perform(get("/courses/{courseId}/gradebook", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheme.method").value("TOTAL_POINTS"))
                .andExpect(jsonPath("$.rows[0].finalGrade.score").value(12.50))
                .andExpect(jsonPath("$.rows[0].finalGrade.roundedScore").value(13))
                .andExpect(jsonPath("$.rows[0].finalGrade.passed").value(true));
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
