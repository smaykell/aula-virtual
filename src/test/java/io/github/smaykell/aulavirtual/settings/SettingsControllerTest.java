package io.github.smaykell.aulavirtual.settings;

import static org.mockito.ArgumentMatchers.any;
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
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import io.github.smaykell.aulavirtual.settings.dto.SettingsResponse;
import io.github.smaykell.aulavirtual.settings.dto.UpdateSettingsRequest;
import java.util.Set;
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

@WebMvcTest(SettingsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private SettingsService settingsService;

    @Test
    void without_a_token_the_configuration_answers_401() throws Exception {
        mockMvc.perform(get("/settings"))
                .andExpect(status().isUnauthorized());

        verify(settingsService, never()).current();
    }

    @Test
    void an_admin_reads_the_configuration() throws Exception {
        when(settingsService.current())
                .thenReturn(new SettingsResponse(StudentIdentifier.DOCUMENT_NUMBER, false));

        mockMvc.perform(get("/settings").header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentIdentifier").value("DOCUMENT_NUMBER"))
                .andExpect(jsonPath("$.selfRegistrationEnabled").value(false));
    }

    @Test
    void an_admin_cannot_change_how_everybody_is_identified() throws Exception {
        mockMvc.perform(put("/settings")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("DOCUMENT_NUMBER", true)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GEN_ACCESS_DENIED"));

        verify(settingsService, never()).update(any());
    }

    @Test
    void the_superadmin_opens_the_self_registration() throws Exception {
        when(settingsService.update(new UpdateSettingsRequest(StudentIdentifier.EMAIL, true)))
                .thenReturn(new SettingsResponse(StudentIdentifier.EMAIL, true));

        mockMvc.perform(put("/settings")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("EMAIL", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selfRegistrationEnabled").value(true));
    }

    @Test
    void a_teacher_does_not_reach_the_configuration() throws Exception {
        mockMvc.perform(get("/settings")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isForbidden());

        verify(settingsService, never()).current();
    }

    @Test
    void an_unknown_identifier_is_rejected_before_reaching_the_service() throws Exception {
        mockMvc.perform(put("/settings")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CARNET_DEL_CLUB", true)))
                .andExpect(status().isBadRequest());

        verify(settingsService, never()).update(any());
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }

    private static String body(String identifier, boolean selfRegistrationEnabled) {
        return """
                {
                  "studentIdentifier": "%s",
                  "selfRegistrationEnabled": %s
                }
                """.formatted(identifier, selfRegistrationEnabled);
    }
}
