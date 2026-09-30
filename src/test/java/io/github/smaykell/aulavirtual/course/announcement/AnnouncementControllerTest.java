package io.github.smaykell.aulavirtual.course.announcement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.course.announcement.dto.AnnouncementData;
import io.github.smaykell.aulavirtual.course.announcement.dto.AnnouncementResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
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

@WebMvcTest(AnnouncementController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class AnnouncementControllerTest {

    private static final UUID COURSE = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AnnouncementService announcementService;

    @Test
    void a_student_reads_the_board_newest_first() throws Exception {
        when(announcementService.list(eq("ana"), eq(COURSE), any()))
                .thenReturn(new PageResponse<>(List.of(anAnnouncement()), 0, 20, 1, 1, true,
                        true));

        mockMvc.perform(get("/courses/{courseId}/announcements", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Examen parcial"))
                .andExpect(jsonPath("$.content[0].authorName").value("Juan Perez"));
    }

    @Test
    void the_teacher_publishes_an_announcement() throws Exception {
        when(announcementService.publish(eq("ana"), eq(COURSE), any(AnnouncementData.class)))
                .thenReturn(anAnnouncement());

        mockMvc.perform(post("/courses/{courseId}/announcements", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Examen parcial", "Será el lunes a las 8.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Examen parcial"));
    }

    @Test
    void a_student_cannot_publish() throws Exception {
        mockMvc.perform(post("/courses/{courseId}/announcements", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Hola", "Hola a todos")))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).publish(any(), any(), any());
    }

    @Test
    void an_announcement_without_text_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/courses/{courseId}/announcements", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Examen parcial", "  ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("body"));

        verify(announcementService, never()).publish(any(), any(), any());
    }

    @Test
    void the_teacher_deletes_an_announcement() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/announcements/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isNoContent());

        verify(announcementService).delete("ana", id);
    }

    private static AnnouncementResponse anAnnouncement() {
        return new AnnouncementResponse(UUID.randomUUID(), COURSE, "Examen parcial",
                "Será el lunes a las 8.", "Juan Perez", Instant.EPOCH, Instant.EPOCH);
    }

    private static String body(String title, String text) {
        return "{\"title\": \"" + title + "\", \"body\": \"" + text + "\"}";
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
