package io.github.smaykell.aulavirtual.modules.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.modules.user.dto.LoginRequest;
import io.github.smaykell.aulavirtual.modules.user.dto.LoginResponse;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.Permission;
import io.github.smaykell.aulavirtual.security.Role;
import io.jsonwebtoken.Claims;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String SECRET = "c2VjcmV0by1kZS1wcnVlYmFzLWNvbi1tYXMtZGUtMzItYnl0ZXM=";
    private static final String ISSUER = "aula-virtual";
    private static final Duration EXPIRATION = Duration.ofHours(1);
    private static final String PASSWORD = "contrasena";

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(new JwtProperties(SECRET, ISSUER, EXPIRATION), Clock.systemUTC());
        authenticationService = new AuthenticationService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void it_issues_a_token_with_the_role_and_its_permissions() {
        givenUser("ana", Role.ADMIN);

        LoginResponse session = authenticationService.login(new LoginRequest("ana", PASSWORD));

        Claims claims = jwtService.verify(session.accessToken()).getPayload();
        assertThat(claims.getSubject()).isEqualTo("ana");
        assertThat(jwtService.authoritiesOf(claims))
                .containsExactlyInAnyOrderElementsOf(Role.ADMIN.grantedAuthorities());
    }

    @Test
    void it_answers_with_the_lifetime_and_the_permissions_of_the_session() {
        givenUser("ana", Role.ADMIN);

        LoginResponse session = authenticationService.login(new LoginRequest("ana", PASSWORD));

        assertThat(session.tokenType()).isEqualTo("Bearer");
        assertThat(session.expiresIn()).isEqualTo(EXPIRATION.toSeconds());
        assertThat(session.role()).isEqualTo(Role.ADMIN);
        assertThat(session.permissions()).containsExactlyInAnyOrderElementsOf(
                Role.ADMIN.permissions().stream().map(Permission::authority).toList());
    }

    @Test
    void the_username_is_normalized_before_looking_it_up() {
        givenUser("ana", Role.TEACHER);

        LoginResponse session = authenticationService.login(new LoginRequest("  ANA ", PASSWORD));

        assertThat(session.username()).isEqualTo("ana");
    }

    @Test
    void an_unknown_user_gets_the_same_answer_as_a_wrong_password() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> authenticationService.login(new LoginRequest("fantasma", PASSWORD)));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(error.getMessage()).isEqualTo("Usuario o contraseña incorrectos");
    }

    @Test
    void a_wrong_password_is_rejected() {
        givenUser("ana", Role.STUDENT);

        ApiException error = assertThrows(ApiException.class,
                () -> authenticationService.login(new LoginRequest("ana", "otra-cosa")));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(error.getMessage()).isEqualTo("Usuario o contraseña incorrectos");
    }

    @Test
    void a_deactivated_user_cannot_log_in() {
        User user = givenUser("ana", Role.TEACHER);
        user.deactivate();

        ApiException error = assertThrows(ApiException.class,
                () -> authenticationService.login(new LoginRequest("ana", PASSWORD)));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private User givenUser(String username, Role role) {
        User user = User.create(username, passwordEncoder.encode(PASSWORD), role);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        return user;
    }
}
