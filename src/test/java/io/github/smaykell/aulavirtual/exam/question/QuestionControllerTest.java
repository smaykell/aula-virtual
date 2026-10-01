package io.github.smaykell.aulavirtual.exam.question;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionResponse;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionData;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
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

@WebMvcTest(QuestionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class QuestionControllerTest {

    private static final UUID COURSE = UUID.randomUUID();
    private static final String TRUE_OR_FALSE = """
            {"type": "TRUE_FALSE", "statement": "Lima es la capital del Perú",
             "statementIsTrue": true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private QuestionService questionService;

    @Test
    void a_teacher_adds_a_question_to_the_bank_and_gets_a_201() throws Exception {
        when(questionService.create(eq("ana"), eq(COURSE), any(QuestionData.class)))
                .thenReturn(aQuestion());

        mockMvc.perform(post("/courses/{courseId}/questions", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRUE_OR_FALSE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TRUE_FALSE"))
                .andExpect(jsonPath("$.options[0].correct").value(true));
    }

    @Test
    void a_student_does_not_read_the_bank() throws Exception {
        mockMvc.perform(get("/courses/{courseId}/questions", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GEN_ACCESS_DENIED"));

        verify(questionService, never()).list(any(), any());
    }

    @Test
    void a_student_does_not_write_questions() throws Exception {
        mockMvc.perform(post("/courses/{courseId}/questions", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRUE_OR_FALSE))
                .andExpect(status().isForbidden());

        verify(questionService, never()).create(any(), any(), any());
    }

    @Test
    void a_question_without_a_type_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/courses/{courseId}/questions", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statement\": \"Sin tipo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("type"));

        verify(questionService, never()).create(any(), any(), any());
    }

    private QuestionResponse aQuestion() {
        return new QuestionResponse(UUID.randomUUID(), COURSE, QuestionType.TRUE_FALSE,
                "Lima es la capital del Perú",
                List.of(new OptionResponse(UUID.randomUUID(), "Verdadero", true),
                        new OptionResponse(UUID.randomUUID(), "Falso", false)),
                null, Instant.EPOCH);
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
