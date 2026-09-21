package io.github.smaykell.aulavirtual.modules.teacher;

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

import io.github.smaykell.aulavirtual.common.domain.Sex;
import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.modules.teacher.dto.CreateTeacherRequest;
import io.github.smaykell.aulavirtual.modules.teacher.dto.TeacherResponse;
import io.github.smaykell.aulavirtual.modules.teacher.dto.UpdateTeacherRequest;
import io.github.smaykell.aulavirtual.modules.user.dto.ChangePasswordRequest;
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

@WebMvcTest(TeacherController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class TeacherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private TeacherService teacherService;

    @Test
    void without_a_token_the_registration_answers_401() throws Exception {
        mockMvc.perform(post("/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("1990-05-20", "contrasena")))
                .andExpect(status().isUnauthorized());

        verify(teacherService, never()).create(any(), any());
    }

    @Test
    void a_teacher_cannot_register_teachers() throws Exception {
        mockMvc.perform(post("/teachers")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("1990-05-20", "contrasena")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GEN_ACCESS_DENIED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        verify(teacherService, never()).create(any(), any());
    }

    @Test
    void an_admin_registers_a_teacher_and_gets_a_201() throws Exception {
        when(teacherService.create(eq("ana"), any(CreateTeacherRequest.class)))
                .thenReturn(new TeacherResponse(UUID.randomUUID(), UUID.randomUUID(),
                        "nuevo.docente", "Juan Carlos", "Perez Gomez", LocalDate.of(1990, 5, 20),
                        Sex.MALE, true, Instant.EPOCH));

        mockMvc.perform(post("/teachers")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("1990-05-20", "contrasena")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nuevo.docente"))
                .andExpect(jsonPath("$.firstName").value("Juan Carlos"))
                .andExpect(jsonPath("$.birthDate").value("1990-05-20"))
                .andExpect(jsonPath("$.sex").value("MALE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void a_birth_date_in_the_future_is_rejected_before_reaching_the_service() throws Exception {
        mockMvc.perform(post("/teachers")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("2999-01-01", "contrasena")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GEN_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("birthDate"));

        verify(teacherService, never()).create(any(), any());
    }

    @Test
    void a_short_password_is_rejected_before_reaching_the_service() throws Exception {
        mockMvc.perform(post("/teachers")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("1990-05-20", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        verify(teacherService, never()).create(any(), any());
    }

    @Test
    void the_first_name_is_mandatory() throws Exception {
        mockMvc.perform(post("/teachers")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithFirstName("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("firstName"));

        verify(teacherService, never()).create(any(), any());
    }

    @Test
    void an_admin_lists_teachers_as_a_page_response() throws Exception {
        when(teacherService.list(eq("ana"), eq(null), any()))
                .thenReturn(new PageResponse<>(List.of(aTeacher(true)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/teachers").header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("nuevo.docente"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void the_active_filter_reaches_the_service() throws Exception {
        when(teacherService.list(eq("ana"), eq(false), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/teachers")
                        .param("active", "false")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk());

        verify(teacherService).list(eq("ana"), eq(false), any());
    }

    @Test
    void an_admin_reads_one_teacher() throws Exception {
        UUID id = UUID.randomUUID();
        when(teacherService.get("ana", id)).thenReturn(aTeacher(true));

        mockMvc.perform(get("/teachers/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Juan Carlos"));
    }

    @Test
    void an_admin_updates_a_teacher() throws Exception {
        UUID id = UUID.randomUUID();
        when(teacherService.update(eq("ana"), eq(id), any(UpdateTeacherRequest.class)))
                .thenReturn(aTeacher(true));

        mockMvc.perform(put("/teachers/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("1990-05-20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Perez Gomez"));
    }

    @Test
    void an_update_with_a_future_birth_date_is_rejected() throws Exception {
        mockMvc.perform(put("/teachers/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("2999-01-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("birthDate"));

        verify(teacherService, never()).update(any(), any(), any());
    }

    @Test
    void an_admin_disables_a_teacher_through_the_dollar_operation() throws Exception {
        UUID id = UUID.randomUUID();
        when(teacherService.disable("ana", id)).thenReturn(aTeacher(false));

        mockMvc.perform(post("/teachers/{id}/$disable", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void an_admin_enables_a_teacher_through_the_dollar_operation() throws Exception {
        UUID id = UUID.randomUUID();
        when(teacherService.enable("ana", id)).thenReturn(aTeacher(true));

        mockMvc.perform(post("/teachers/{id}/$enable", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void a_teacher_cannot_disable_teachers() throws Exception {
        mockMvc.perform(post("/teachers/{id}/$disable", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isForbidden());

        verify(teacherService, never()).disable(any(), any());
    }

    @Test
    void an_admin_changes_the_password_of_a_teacher() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/teachers/{id}/$changePassword", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("contrasena")))
                .andExpect(status().isNoContent());

        verify(teacherService).changePassword(eq("ana"), eq(id),
                any(ChangePasswordRequest.class));
    }

    @Test
    void a_short_new_password_is_rejected_before_reaching_the_service() throws Exception {
        mockMvc.perform(post("/teachers/{id}/$changePassword", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        verify(teacherService, never()).changePassword(any(), any(), any());
    }

    @Test
    void a_teacher_cannot_change_the_password_of_teachers() throws Exception {
        mockMvc.perform(post("/teachers/{id}/$changePassword", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("contrasena")))
                .andExpect(status().isForbidden());

        verify(teacherService, never()).changePassword(any(), any(), any());
    }

    @Test
    void a_student_cannot_list_teachers() throws Exception {
        mockMvc.perform(get("/teachers").header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isForbidden());

        verify(teacherService, never()).list(any(), any(), any());
    }

    private TeacherResponse aTeacher(boolean active) {
        return new TeacherResponse(UUID.randomUUID(), UUID.randomUUID(), "nuevo.docente",
                "Juan Carlos", "Perez Gomez", LocalDate.of(1990, 5, 20), Sex.MALE, active,
                Instant.EPOCH);
    }

    private String passwordBody(String password) {
        return """
                {"password": "%s"}
                """.formatted(password);
    }

    private String updateBody(String birthDate) {
        return """
                {"firstName": "Juan Carlos", "lastName": "Perez Gomez", "birthDate": "%s",                 "sex": "MALE"}
                """.formatted(birthDate);
    }

    private String registrationBody(String birthDate, String password) {
        return """
                {"firstName": "Juan Carlos", "lastName": "Perez Gomez", "birthDate": "%s", \
                "sex": "MALE", "username": "nuevo.docente", "password": "%s"}
                """.formatted(birthDate, password);
    }

    private String bodyWithFirstName(String firstName) {
        return """
                {"firstName": "%s", "lastName": "Perez Gomez", "birthDate": "1990-05-20", \
                "sex": "MALE", "username": "nuevo.docente", "password": "contrasena"}
                """.formatted(firstName);
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", role.grantedAuthorities());
    }
}
