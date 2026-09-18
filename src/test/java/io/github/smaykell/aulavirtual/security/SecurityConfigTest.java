package io.github.smaykell.aulavirtual.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.config.CorsProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifica la cadena de seguridad completa sobre peticiones HTTP reales, sin
 * necesidad de base de datos: rodaja web, controlador de prueba y token de verdad.
 */
@WebMvcTest
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SecurityConfigTest.ControladorDePrueba.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void sin_token_responde_401_con_el_formato_de_error_de_la_api() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/ping"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Se requiere un token de acceso valido"));
    }

    @Test
    void con_un_token_valido_deja_pasar_la_peticion() throws Exception {
        String token = jwtService.generarToken("ana@aula.test", List.of("ROLE_DOCENTE"));

        mockMvc.perform(get("/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void el_subject_del_token_llega_al_controlador() throws Exception {
        String token = jwtService.generarToken("ana@aula.test", List.of("ROLE_DOCENTE"));

        mockMvc.perform(get("/quien-soy").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string("ana@aula.test"));
    }

    @Test
    void un_token_invalido_se_trata_como_ausencia_de_token() throws Exception {
        mockMvc.perform(get("/ping").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es.un.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sin_la_authority_exigida_responde_403_con_el_formato_de_error_de_la_api()
            throws Exception {
        String token = jwtService.generarToken("ana@aula.test", List.of("ROLE_ESTUDIANTE"));

        mockMvc.perform(get("/solo-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void con_la_authority_exigida_permite_el_acceso() throws Exception {
        String token = jwtService.generarToken("root@aula.test", List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/solo-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    /** Endpoints minimos que existen solo para ejercitar la cadena de seguridad. */
    @RestController
    static class ControladorDePrueba {

        @GetMapping("/ping")
        String ping() {
            return "pong";
        }

        @GetMapping("/quien-soy")
        String quienSoy(java.security.Principal principal) {
            return principal.getName();
        }

        @GetMapping("/solo-admin")
        @PreAuthorize("hasRole('ADMIN')")
        String soloAdmin() {
            return "ok";
        }
    }
}
