package io.github.smaykell.aulavirtual.modules.student;

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

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.modules.person.DocumentType;
import io.github.smaykell.aulavirtual.modules.person.Sex;
import io.github.smaykell.aulavirtual.modules.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.modules.student.dto.CreateStudentRequest;
import io.github.smaykell.aulavirtual.modules.student.dto.StudentResponse;
import io.github.smaykell.aulavirtual.modules.student.dto.UpdateStudentRequest;
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

@WebMvcTest(StudentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private StudentService studentService;

    @Test
    void without_a_token_the_registration_answers_401() throws Exception {
        mockMvc.perform(post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("1990-05-20", "contrasena")))
                .andExpect(status().isUnauthorized());

        verify(studentService, never()).create(any(), any());
    }

    @Test
    void a_teacher_cannot_register_students() throws Exception {
        mockMvc.perform(post("/students")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("1990-05-20", "contrasena")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GEN_ACCESS_DENIED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        verify(studentService, never()).create(any(), any());
    }

    @Test
    void an_admin_registers_a_student_and_gets_a_201() throws Exception {
        when(studentService.create(eq("ana"), any(CreateStudentRequest.class)))
                .thenReturn(aStudent(true));

        mockMvc.perform(post("/students")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("1990-05-20", "contrasena")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nuevo.docente"))
                .andExpect(jsonPath("$.person.firstName").value("Juan Carlos"))
                .andExpect(jsonPath("$.person.documentNumber").value("45678912"))
                .andExpect(jsonPath("$.person.birthDate").value("1990-05-20"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void a_registration_without_credentials_reaches_the_service() throws Exception {
        when(studentService.create(eq("ana"), any(CreateStudentRequest.class)))
                .thenReturn(aStudent(true));

        mockMvc.perform(post("/students")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"person\": " + personBody("1990-05-20") + "}"))
                .andExpect(status().isCreated());

        verify(studentService).create(eq("ana"), any(CreateStudentRequest.class));
    }

    @Test
    void a_birth_date_in_the_future_is_rejected_before_reaching_the_service() throws Exception {
        mockMvc.perform(post("/students")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("2999-01-01", "contrasena")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GEN_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("person.birthDate"));

        verify(studentService, never()).create(any(), any());
    }

    @Test
    void a_short_password_is_rejected_before_reaching_the_service() throws Exception {
        mockMvc.perform(post("/students")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("1990-05-20", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("credentials.password"));

        verify(studentService, never()).create(any(), any());
    }

    @Test
    void the_document_number_is_mandatory() throws Exception {
        mockMvc.perform(post("/students")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithDocumentNumber("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("person.documentNumber"));

        verify(studentService, never()).create(any(), any());
    }

    @Test
    void an_admin_lists_students_as_a_page_response() throws Exception {
        when(studentService.list(eq("ana"), eq(null), any()))
                .thenReturn(new PageResponse<>(List.of(aStudent(true)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/students").header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("nuevo.docente"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void the_active_filter_reaches_the_service() throws Exception {
        when(studentService.list(eq("ana"), eq(false), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/students")
                        .param("active", "false")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk());

        verify(studentService).list(eq("ana"), eq(false), any());
    }

    @Test
    void an_admin_reads_one_student() throws Exception {
        UUID id = UUID.randomUUID();
        when(studentService.get("ana", id)).thenReturn(aStudent(true));

        mockMvc.perform(get("/students/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.person.firstName").value("Juan Carlos"));
    }

    @Test
    void an_admin_updates_a_student() throws Exception {
        UUID id = UUID.randomUUID();
        when(studentService.update(eq("ana"), eq(id), any(UpdateStudentRequest.class)))
                .thenReturn(aStudent(true));

        mockMvc.perform(put("/students/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("1990-05-20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.person.lastName").value("Perez Gomez"));
    }

    @Test
    void an_update_with_a_future_birth_date_is_rejected() throws Exception {
        mockMvc.perform(put("/students/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("2999-01-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("person.birthDate"));

        verify(studentService, never()).update(any(), any(), any());
    }

    @Test
    void an_admin_disables_a_student_through_the_dollar_operation() throws Exception {
        UUID id = UUID.randomUUID();
        when(studentService.disable("ana", id)).thenReturn(aStudent(false));

        mockMvc.perform(post("/students/{id}/$disable", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void an_admin_enables_a_student_through_the_dollar_operation() throws Exception {
        UUID id = UUID.randomUUID();
        when(studentService.enable("ana", id)).thenReturn(aStudent(true));

        mockMvc.perform(post("/students/{id}/$enable", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void a_teacher_cannot_disable_students() throws Exception {
        mockMvc.perform(post("/students/{id}/$disable", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isForbidden());

        verify(studentService, never()).disable(any(), any());
    }

    @Test
    void an_admin_changes_the_password_of_a_student() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/students/{id}/$changePassword", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("contrasena")))
                .andExpect(status().isNoContent());

        verify(studentService).changePassword(eq("ana"), eq(id),
                any(ChangePasswordRequest.class));
    }

    @Test
    void a_short_new_password_is_rejected_before_reaching_the_service() throws Exception {
        mockMvc.perform(post("/students/{id}/$changePassword", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        verify(studentService, never()).changePassword(any(), any(), any());
    }

    @Test
    void a_student_cannot_list_students() throws Exception {
        mockMvc.perform(get("/students")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isForbidden());

        verify(studentService, never()).list(any(), any(), any());
    }

    private StudentResponse aStudent(boolean active) {
        return new StudentResponse(UUID.randomUUID(),
                new PersonResponse(UUID.randomUUID(), DocumentType.DNI, "45678912", "Juan Carlos",
                        "Perez Gomez", LocalDate.of(1990, 5, 20), Sex.MALE),
                "nuevo.docente", active, Instant.EPOCH);
    }

    private String passwordBody(String password) {
        return """
                {"password": "%s"}
                """.formatted(password);
    }

    private String personBody(String birthDate) {
        return """
                {"documentType": "DNI", "documentNumber": "45678912",
                 "firstName": "Juan Carlos", "lastName": "Perez Gomez",
                 "birthDate": "%s", "sex": "MALE"}
                """.formatted(birthDate);
    }

    private String updateBody(String birthDate) {
        return """
                {"person": %s}
                """.formatted(personBody(birthDate));
    }

    private String registrationBody(String birthDate, String password) {
        return """
                {"person": %s,
                 "credentials": {"username": "nuevo.docente", "password": "%s"}}
                """.formatted(personBody(birthDate), password);
    }

    private String bodyWithDocumentNumber(String documentNumber) {
        return """
                {"person": {"documentType": "DNI", "documentNumber": "%s",
                            "firstName": "Juan Carlos", "lastName": "Perez Gomez",
                            "birthDate": "1990-05-20", "sex": "MALE"},
                 "credentials": {"username": "nuevo.docente", "password": "contrasena"}}
                """.formatted(documentNumber);
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
