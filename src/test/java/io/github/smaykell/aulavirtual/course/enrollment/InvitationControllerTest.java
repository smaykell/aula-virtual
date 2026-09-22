package io.github.smaykell.aulavirtual.course.enrollment;

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
import io.github.smaykell.aulavirtual.course.enrollment.dto.CourseInvitationResponse;
import io.github.smaykell.aulavirtual.course.enrollment.dto.SelfRegistrationResponse;
import io.github.smaykell.aulavirtual.course.exception.AccountAlreadyRegisteredException;
import io.github.smaykell.aulavirtual.course.exception.InvalidInvitationException;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import io.github.smaykell.aulavirtual.settings.StudentIdentifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvitationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SelfEnrollmentService selfEnrollmentService;

    @Test
    void anybody_with_the_link_sees_the_course_without_logging_in() throws Exception {
        when(selfEnrollmentService.preview("ABCD2345")).thenReturn(new CourseInvitationResponse(
                "Algebra Lineal", "Juan Perez", StudentIdentifier.DOCUMENT_NUMBER, true));

        mockMvc.perform(get("/invitations/{code}", "ABCD2345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseName").value("Algebra Lineal"))
                .andExpect(jsonPath("$.teacherName").value("Juan Perez"))
                .andExpect(jsonPath("$.studentIdentifier").value("DOCUMENT_NUMBER"))
                .andExpect(jsonPath("$.open").value(true));
    }

    @Test
    void the_preview_does_not_give_away_anything_else_of_the_course() throws Exception {
        when(selfEnrollmentService.preview("ABCD2345")).thenReturn(new CourseInvitationResponse(
                "Algebra Lineal", "Juan Perez", StudentIdentifier.DOCUMENT_NUMBER, true));

        mockMvc.perform(get("/invitations/{code}", "ABCD2345"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.startDate").doesNotExist())
                .andExpect(jsonPath("$.enrollmentPolicy").doesNotExist());
    }

    @Test
    void a_code_that_belongs_to_no_course_answers_404_and_not_401() throws Exception {
        when(selfEnrollmentService.preview("ZZZZ9999")).thenThrow(new InvalidInvitationException());

        mockMvc.perform(get("/invitations/{code}", "ZZZZ9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CRS_INVALID_INVITATION"));
    }

    @Test
    void registering_without_a_token_creates_the_account_and_answers_201() throws Exception {
        when(selfEnrollmentService.register(eq("ABCD2345"), any())).thenReturn(
                new SelfRegistrationResponse("45678912", EnrollmentStatus.ACTIVE));

        mockMvc.perform(post("/invitations/{code}/$register", "ABCD2345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("45678912", "ana@escuela.pe")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("45678912"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void the_answer_never_carries_the_password_back() throws Exception {
        when(selfEnrollmentService.register(eq("ABCD2345"), any())).thenReturn(
                new SelfRegistrationResponse("45678912", EnrollmentStatus.ACTIVE));

        mockMvc.perform(post("/invitations/{code}/$register", "ABCD2345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("45678912", "ana@escuela.pe")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void a_body_without_the_workplace_answers_400_which_is_what_proves_the_route_is_public()
            throws Exception {

        mockMvc.perform(post("/invitations/{code}/$register", "ABCD2345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "person": {
                                    "documentType": "DNI",
                                    "documentNumber": "45678912",
                                    "firstName": "Ana Maria",
                                    "lastName": "Quispe Rojas",
                                    "birthDate": "1990-05-20",
                                    "sex": "FEMALE",
                                    "email": "ana@escuela.pe"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("workplace"));

        verify(selfEnrollmentService, never()).register(any(), any());
    }

    @Test
    void a_document_already_registered_is_sent_to_the_login_with_one_single_code()
            throws Exception {

        when(selfEnrollmentService.register(eq("ABCD2345"), any()))
                .thenThrow(new AccountAlreadyRegisteredException());

        mockMvc.perform(post("/invitations/{code}/$register", "ABCD2345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("45678912", "ana@escuela.pe")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CRS_ACCOUNT_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private static String registrationBody(String documentNumber, String email) {
        return """
                {
                  "person": {
                    "documentType": "DNI",
                    "documentNumber": "%s",
                    "firstName": "Ana Maria",
                    "lastName": "Quispe Rojas",
                    "birthDate": "1990-05-20",
                    "sex": "FEMALE",
                    "email": "%s"
                  },
                  "workplace": "Hospital Regional"
                }
                """.formatted(documentNumber, email);
    }
}
