package io.github.smaykell.aulavirtual.modules.course;

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
import io.github.smaykell.aulavirtual.modules.course.dto.CourseResponse;
import io.github.smaykell.aulavirtual.modules.course.dto.CreateCourseRequest;
import io.github.smaykell.aulavirtual.modules.course.dto.InvitationResponse;
import io.github.smaykell.aulavirtual.modules.course.dto.UpdateCourseRequest;
import io.github.smaykell.aulavirtual.modules.teacher.dto.TeacherSummary;
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

@WebMvcTest(CourseController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CourseService courseService;

    @Test
    void without_a_token_the_listing_answers_401() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isUnauthorized());

        verify(courseService, never()).list(any(), any(), any(), any());
    }

    @Test
    void a_student_lists_courses_and_the_service_decides_which_ones() throws Exception {
        when(courseService.list(eq("ana"), eq(null), eq(null), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/courses").header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isOk());

        verify(courseService).list(eq("ana"), eq(null), eq(null), any());
    }

    @Test
    void a_student_cannot_create_a_course() throws Exception {
        mockMvc.perform(post("/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GEN_ACCESS_DENIED"));

        verify(courseService, never()).create(any(), any());
    }

    @Test
    void a_teacher_creates_a_course_and_gets_a_201() throws Exception {
        when(courseService.create(eq("ana"), any(CreateCourseRequest.class)))
                .thenReturn(aCourse(CourseStatus.ACTIVE));

        mockMvc.perform(post("/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Algebra Lineal"))
                .andExpect(jsonPath("$.invitation.code").value("ABCD2345"))
                .andExpect(jsonPath("$.invitation.url")
                        .value("https://aula.example/join/ABCD2345"))
                .andExpect(jsonPath("$.teacher.lastName").value("Perez Gomez"));
    }

    @Test
    void a_course_without_a_name_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enrollmentPolicy\": \"ON_REQUEST\","
                                + " \"startDate\": \"2026-03-01\", \"endDate\": \"2026-07-15\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GEN_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));

        verify(courseService, never()).create(any(), any());
    }

    @Test
    void the_filters_of_the_listing_reach_the_service() throws Exception {
        UUID teacherId = UUID.randomUUID();
        when(courseService.list(eq("ana"), eq(teacherId), eq(CourseStatus.ARCHIVED), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/courses")
                        .param("teacherId", teacherId.toString())
                        .param("status", "ARCHIVED")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk());

        verify(courseService).list(eq("ana"), eq(teacherId), eq(CourseStatus.ARCHIVED), any());
    }

    @Test
    void an_update_without_a_titular_does_not_reach_the_service() throws Exception {
        mockMvc.perform(put("/courses/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("teacherId"));

        verify(courseService, never()).update(any(), any(), any(UpdateCourseRequest.class));
    }

    @Test
    void a_course_is_archived_and_activated_through_the_dollar_operations() throws Exception {
        UUID id = UUID.randomUUID();
        when(courseService.archive("ana", id)).thenReturn(aCourse(CourseStatus.ARCHIVED));
        when(courseService.activate("ana", id)).thenReturn(aCourse(CourseStatus.ACTIVE));

        mockMvc.perform(post("/courses/{id}/$archive", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(post("/courses/{id}/$activate", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private CourseResponse aCourse(CourseStatus status) {
        return new CourseResponse(UUID.randomUUID(), "Algebra Lineal", "Curso del primer ciclo",
                new TeacherSummary(UUID.randomUUID(), "Juan Carlos", "Perez Gomez", true),
                new InvitationResponse("ABCD2345", "https://aula.example/join/ABCD2345"),
                status, EnrollmentPolicy.ON_REQUEST, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 7, 15), Instant.EPOCH);
    }

    private String courseBody() {
        return """
                {"name": "Algebra Lineal", "description": "Curso del primer ciclo",
                 "enrollmentPolicy": "ON_REQUEST",
                 "startDate": "2026-03-01", "endDate": "2026-07-15"}
                """;
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
