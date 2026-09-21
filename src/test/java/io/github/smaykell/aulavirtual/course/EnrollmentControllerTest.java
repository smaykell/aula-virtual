package io.github.smaykell.aulavirtual.course;

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
import io.github.smaykell.aulavirtual.course.dto.CourseSummary;
import io.github.smaykell.aulavirtual.course.dto.EnrollmentResponse;
import io.github.smaykell.aulavirtual.course.dto.JoinCourseRequest;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
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

@WebMvcTest(EnrollmentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class EnrollmentControllerTest {

    private static final UUID COURSE = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    void without_a_token_joining_a_course_answers_401() throws Exception {
        mockMvc.perform(post("/courses/$join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody("ABCD2345")))
                .andExpect(status().isUnauthorized());

        verify(enrollmentService, never()).join(any(), any());
    }

    @Test
    void a_student_joins_a_course_and_gets_a_201() throws Exception {
        when(enrollmentService.join(eq("ana"), any(JoinCourseRequest.class)))
                .thenReturn(anEnrollment(EnrollmentStatus.PENDING));

        mockMvc.perform(post("/courses/$join")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody("ABCD2345")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.course.name").value("Algebra Lineal"))
                .andExpect(jsonPath("$.student.lastName").value("Quispe Rojas"));
    }

    @Test
    void a_teacher_does_not_enroll_itself_in_a_course() throws Exception {
        mockMvc.perform(post("/courses/$join")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody("ABCD2345")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GEN_ACCESS_DENIED"));

        verify(enrollmentService, never()).join(any(), any());
    }

    @Test
    void joining_without_a_code_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/courses/$join")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("code"));

        verify(enrollmentService, never()).join(any(), any());
    }

    @Test
    void the_teacher_reads_the_roster_of_its_course_filtered_by_status() throws Exception {
        when(enrollmentService.list(eq("ana"), eq(COURSE), eq(EnrollmentStatus.PENDING), any()))
                .thenReturn(new PageResponse<>(List.of(anEnrollment(EnrollmentStatus.PENDING)),
                        0, 20, 1, 1, true, true));

        mockMvc.perform(get("/courses/{courseId}/enrollments", COURSE)
                        .param("status", "PENDING")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void a_student_does_not_read_the_roster() throws Exception {
        mockMvc.perform(get("/courses/{courseId}/enrollments", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isForbidden());

        verify(enrollmentService, never()).list(any(), any(), any(), any());
    }

    @Test
    void the_teacher_accepts_and_rejects_through_the_dollar_operations() throws Exception {
        UUID id = UUID.randomUUID();
        when(enrollmentService.accept("ana", id)).thenReturn(anEnrollment(EnrollmentStatus.ACTIVE));
        when(enrollmentService.reject("ana", id))
                .thenReturn(anEnrollment(EnrollmentStatus.REJECTED));

        mockMvc.perform(post("/enrollments/{id}/$accept", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/enrollments/{id}/$reject", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void a_student_does_not_accept_its_own_request() throws Exception {
        mockMvc.perform(post("/enrollments/{id}/$accept", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isForbidden());

        verify(enrollmentService, never()).accept(any(), any());
    }

    @Test
    void the_teacher_withdraws_a_student_from_the_course() throws Exception {
        UUID id = UUID.randomUUID();
        when(enrollmentService.withdraw("ana", id))
                .thenReturn(anEnrollment(EnrollmentStatus.WITHDRAWN));

        mockMvc.perform(post("/enrollments/{id}/$withdraw", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }

    private EnrollmentResponse anEnrollment(EnrollmentStatus status) {
        return new EnrollmentResponse(UUID.randomUUID(),
                new CourseSummary(COURSE, "Algebra Lineal", CourseStatus.ACTIVE),
                new StudentSummary(UUID.randomUUID(), "Ana Maria", "Quispe Rojas", true),
                status, Instant.EPOCH, null);
    }

    private String joinBody(String code) {
        return """
                {"code": "%s"}
                """.formatted(code);
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
