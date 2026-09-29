package io.github.smaykell.aulavirtual.gradebook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeCategoryResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeData;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GradingSchemeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class GradingSchemeControllerTest {

    private static final UUID COURSE = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private GradingSchemeService schemeService;

    @Test
    void the_student_reads_how_its_course_is_graded() throws Exception {
        when(schemeService.get("ana", COURSE)).thenReturn(aScheme());

        mockMvc.perform(get("/courses/{courseId}/grading-scheme", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("WEIGHTED"))
                .andExpect(jsonPath("$.categories[0].name").value("Tareas"));
    }

    @Test
    void the_teacher_replaces_the_scheme_of_its_course() throws Exception {
        when(schemeService.replace(eq("ana"), eq(COURSE), any(GradingSchemeData.class)))
                .thenReturn(aScheme());

        mockMvc.perform(put("/courses/{courseId}/grading-scheme", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "WEIGHTED", "passingScore": 13,
                                 "categories": [{"name": "Tareas", "weight": 100}]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void a_student_does_not_change_the_scheme() throws Exception {
        mockMvc.perform(put("/courses/{courseId}/grading-scheme", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "TOTAL_POINTS", "passingScore": 13, "categories": []}
                                """))
                .andExpect(status().isForbidden());

        verify(schemeService, never()).replace(any(), any(), any());
    }

    @Test
    void a_passing_score_over_twenty_does_not_reach_the_service() throws Exception {
        mockMvc.perform(put("/courses/{courseId}/grading-scheme", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "TOTAL_POINTS", "passingScore": 21, "categories": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("passingScore"));

        verify(schemeService, never()).replace(any(), any(), any());
    }

    @Test
    void a_category_without_a_name_does_not_reach_the_service() throws Exception {
        mockMvc.perform(put("/courses/{courseId}/grading-scheme", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "WEIGHTED", "passingScore": 13,
                                 "categories": [{"name": " ", "weight": 100}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("categories[0].name"));

        verify(schemeService, never()).replace(any(), any(), any());
    }

    private static GradingSchemeResponse aScheme() {
        return new GradingSchemeResponse(GradingMethod.WEIGHTED, new BigDecimal("13.00"),
                List.of(new GradeCategoryResponse(UUID.randomUUID(), "Tareas",
                        new BigDecimal("100.00"), 1)));
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
