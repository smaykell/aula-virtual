package io.github.smaykell.aulavirtual.user;

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
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.person.Sex;
import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import io.github.smaykell.aulavirtual.user.dto.ChangeMyPasswordRequest;
import io.github.smaykell.aulavirtual.user.dto.MeResponse;
import io.github.smaykell.aulavirtual.user.dto.RoleAccess;
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

@WebMvcTest(MeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private MeService meService;

    @Test
    void without_a_token_it_answers_401() throws Exception {
        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());

        verify(meService, never()).get(any());
    }

    @Test
    void a_student_reads_its_own_profile_without_any_permission() throws Exception {
        when(meService.get("ana")).thenReturn(me(Role.STUDENT));

        mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.person.firstName").value("Ana Maria"))
                .andExpect(jsonPath("$.roles[0].role").value("STUDENT"));
    }

    @Test
    void a_person_with_two_profiles_sees_both_roles() throws Exception {
        when(meService.get("ana")).thenReturn(new MeResponse("ana",
                RoleAccess.of(List.of(Role.ADMIN, Role.TEACHER)), person()));

        mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.roles[1].role").value("TEACHER"));
    }

    @Test
    void a_teacher_updates_its_own_data() throws Exception {
        when(meService.update(eq("ana"), any(PersonData.class))).thenReturn(me(Role.TEACHER));

        mockMvc.perform(put("/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personBody("1990-05-20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.person.lastName").value("Lopez Diaz"));
    }

    @Test
    void an_update_with_a_future_birth_date_is_rejected() throws Exception {
        mockMvc.perform(put("/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personBody("2999-01-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("birthDate"));

        verify(meService, never()).update(any(), any());
    }

    @Test
    void changing_my_password_answers_204() throws Exception {
        mockMvc.perform(post("/me/$changePassword")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "contrasena",
                                 "newPassword": "contrasena-nueva"}
                                """))
                .andExpect(status().isNoContent());

        verify(meService).changePassword(eq("ana"), any(ChangeMyPasswordRequest.class));
    }

    @Test
    void a_short_new_password_is_rejected_before_reaching_the_service() throws Exception {
        mockMvc.perform(post("/me/$changePassword")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "contrasena", "newPassword": "corta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"));

        verify(meService, never()).changePassword(any(), any());
    }

    private MeResponse me(Role role) {
        return new MeResponse("ana", RoleAccess.of(List.of(role)), person());
    }

    private PersonResponse person() {
        return new PersonResponse(UUID.randomUUID(), DocumentType.DNI, "45678912", "Ana Maria",
                "Lopez Diaz", LocalDate.of(1990, 5, 20), Sex.FEMALE, null);
    }

    private String personBody(String birthDate) {
        return """
                {"documentType": "DNI", "documentNumber": "45678912",
                 "firstName": "Ana Maria", "lastName": "Lopez Diaz",
                 "birthDate": "%s", "sex": "FEMALE"}
                """.formatted(birthDate);
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
