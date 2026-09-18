package io.github.smaykell.aulavirtual.modules.user;

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
import io.github.smaykell.aulavirtual.modules.user.dto.CreateUserRequest;
import io.github.smaykell.aulavirtual.modules.user.dto.UserResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import java.time.Instant;
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

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @Test
    void without_a_token_the_listing_answers_401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).list(any(), any(), any());
    }

    @Test
    void a_student_cannot_list_users() throws Exception {
        mockMvc.perform(get("/users").header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GEN_ACCESS_DENIED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        verify(userService, never()).list(any(), any(), any());
    }

    @Test
    void a_teacher_cannot_create_users() throws Exception {
        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(creationBody("nuevo", "contrasena", "STUDENT")))
                .andExpect(status().isForbidden());

        verify(userService, never()).create(any(), any());
    }

    @Test
    void an_admin_lists_users_as_a_page_response() throws Exception {
        when(userService.list(eq("ana"), eq(null), any())).thenReturn(onePageWith(
                new UserResponse(UUID.randomUUID(), "docente", Role.TEACHER, true, Instant.EPOCH)));

        mockMvc.perform(get("/users").header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("docente"))
                .andExpect(jsonPath("$.content[0].role").value("TEACHER"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void the_active_filter_reaches_the_service() throws Exception {
        when(userService.list(eq("ana"), eq(false), any())).thenReturn(onePageWith());

        mockMvc.perform(get("/users")
                        .param("active", "false")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk());

        verify(userService).list(eq("ana"), eq(false), any());
    }

    @Test
    void an_admin_creates_a_user_and_gets_a_201() throws Exception {
        UserResponse created =
                new UserResponse(UUID.randomUUID(), "nuevo", Role.STUDENT, true, Instant.EPOCH);
        when(userService.create(eq("ana"), any(CreateUserRequest.class))).thenReturn(created);

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(creationBody("nuevo", "contrasena", "STUDENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nuevo"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void a_short_password_is_rejected_before_reaching_the_service() throws Exception {
        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(creationBody("nuevo", "corta", "STUDENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        verify(userService, never()).create(any(), any());
    }

    @Test
    void a_username_with_spaces_is_rejected() throws Exception {
        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(creationBody("con espacios", "contrasena", "STUDENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("username"));

        verify(userService, never()).create(any(), any());
    }

    @Test
    void an_admin_disables_a_user_through_the_dollar_operation() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.disable("ana", id))
                .thenReturn(new UserResponse(id, "docente", Role.TEACHER, false, Instant.EPOCH));

        mockMvc.perform(post("/users/{id}/$disable", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void an_admin_enables_a_user_through_the_dollar_operation() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.enable("ana", id))
                .thenReturn(new UserResponse(id, "docente", Role.TEACHER, true, Instant.EPOCH));

        mockMvc.perform(post("/users/{id}/$enable", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void a_teacher_cannot_disable_users() throws Exception {
        mockMvc.perform(post("/users/{id}/$disable", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isForbidden());

        verify(userService, never()).disable(any(), any());
    }

    @Test
    void an_id_that_is_not_a_uuid_is_a_400() throws Exception {
        mockMvc.perform(post("/users/no-es-un-uuid/$disable")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).disable(any(), any());
    }

    private String creationBody(String username, String password, String role) {
        return """
                {"username": "%s", "password": "%s", "role": "%s"}
                """.formatted(username, password, role);
    }

    private PageResponse<UserResponse> onePageWith(UserResponse... users) {
        List<UserResponse> content = List.of(users);
        return new PageResponse<>(content, 0, 20, content.size(), 1, true, true);
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", role.grantedAuthorities());
    }
}
