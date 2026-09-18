package io.github.smaykell.aulavirtual.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class, SecurityConfigTest.ProbeController.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void without_a_token_it_answers_401_in_the_api_error_format() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/ping"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Se requiere un token de acceso válido"));
    }

    @Test
    void with_a_valid_token_it_lets_the_request_through() throws Exception {
        mockMvc.perform(get("/ping").header(HttpHeaders.AUTHORIZATION, bearerFor("ROLE_TEACHER")))
                .andExpect(status().isOk());
    }

    @Test
    void the_token_subject_reaches_the_controller() throws Exception {
        mockMvc.perform(get("/who-am-i")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor("ROLE_TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string("ana@aula.test"));
    }

    @Test
    void an_invalid_token_is_treated_as_a_missing_token() throws Exception {
        mockMvc.perform(get("/ping").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es.un.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void without_the_required_authority_it_answers_403_in_the_api_error_format() throws Exception {
        mockMvc.perform(get("/admin-only")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor("ROLE_STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.message")
                        .value("No tienes permisos para realizar esta acción"));
    }

    @Test
    void with_the_required_authority_it_allows_access() throws Exception {
        mockMvc.perform(get("/admin-only")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor("ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    private String bearerFor(String authority) {
        return "Bearer " + jwtService.issueToken("ana@aula.test", List.of(authority));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/ping")
        String ping() {
            return "pong";
        }

        @GetMapping("/who-am-i")
        String whoAmI(Principal principal) {
            return principal.getName();
        }

        @GetMapping("/admin-only")
        @PreAuthorize("hasRole('ADMIN')")
        String adminOnly() {
            return "ok";
        }
    }
}
