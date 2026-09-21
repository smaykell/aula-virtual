package io.github.smaykell.aulavirtual.modules.administrator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.modules.administrator.dto.AdministratorResponse;
import io.github.smaykell.aulavirtual.modules.administrator.dto.CreateAdministratorRequest;
import io.github.smaykell.aulavirtual.modules.person.DocumentType;
import io.github.smaykell.aulavirtual.modules.person.Sex;
import io.github.smaykell.aulavirtual.modules.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import java.time.Instant;
import java.time.LocalDate;
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

@WebMvcTest(AdministratorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class AdministratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AdministratorService administratorService;

    @Test
    void without_a_token_it_answers_401() throws Exception {
        mockMvc.perform(get("/administrators"))
                .andExpect(status().isUnauthorized());

        verify(administratorService, never()).list(any(), any(), any());
    }

    @Test
    void an_admin_cannot_see_the_administrators() throws Exception {
        mockMvc.perform(get("/administrators")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GEN_ACCESS_DENIED"));

        verify(administratorService, never()).list(any(), any(), any());
    }

    @Test
    void the_superadmin_lists_the_administrators() throws Exception {
        when(administratorService.list(eq("ana"), eq(null), any()))
                .thenReturn(new PageResponse<>(List.of(anAdministrator(true)), 0, 20, 1, 1, true,
                        true));

        mockMvc.perform(get("/administrators")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.SUPER_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("nuevo.admin"))
                .andExpect(jsonPath("$.content[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.content[0].person.firstName").value("Ana Maria"));
    }

    @Test
    void the_superadmin_registers_an_administrator_and_gets_a_201() throws Exception {
        when(administratorService.create(eq("ana"), any(CreateAdministratorRequest.class)))
                .thenReturn(anAdministrator(true));

        mockMvc.perform(post("/administrators")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("1990-05-20")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nuevo.admin"));
    }

    @Test
    void a_registration_without_the_person_is_rejected() throws Exception {
        mockMvc.perform(post("/administrators")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("person"));

        verify(administratorService, never()).create(any(), any());
    }

    @Test
    void the_superadmin_disables_an_administrator_through_the_dollar_operation() throws Exception {
        UUID id = UUID.randomUUID();
        when(administratorService.disable("ana", id)).thenReturn(anAdministrator(false));

        mockMvc.perform(post("/administrators/{id}/$disable", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.SUPER_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    private AdministratorResponse anAdministrator(boolean active) {
        return new AdministratorResponse(UUID.randomUUID(),
                new PersonResponse(UUID.randomUUID(), DocumentType.DNI, "45678912", "Ana Maria",
                        "Lopez Diaz", LocalDate.of(1990, 5, 20), Sex.FEMALE),
                "nuevo.admin", Role.ADMIN, active, Instant.EPOCH);
    }

    private String registrationBody(String birthDate) {
        return """
                {"person": {"documentType": "DNI", "documentNumber": "45678912",
                            "firstName": "Ana Maria", "lastName": "Lopez Diaz",
                            "birthDate": "%s", "sex": "FEMALE"},
                 "credentials": {"username": "nuevo.admin", "password": "contrasena"}}
                """.formatted(birthDate);
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
