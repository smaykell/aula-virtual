package io.github.smaykell.aulavirtual.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.PersonProfiles;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.dto.LoginRequest;
import io.github.smaykell.aulavirtual.user.dto.LoginResponse;
import io.github.smaykell.aulavirtual.user.dto.RoleAccess;
import io.jsonwebtoken.Claims;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String SECRET = "c2VjcmV0by1kZS1wcnVlYmFzLWNvbi1tYXMtZGUtMzItYnl0ZXM=";
    private static final String ISSUER = "aula-virtual";
    private static final Duration EXPIRATION = Duration.ofHours(1);
    private static final String PASSWORD = "contrasena";
    private static final UUID PERSON = UUID.randomUUID();
    private static final int MAX_FAILURES = 3;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCKOUT = Duration.ofMinutes(10);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonProfiles personProfiles;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private MutableClock clock;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        clock = new MutableClock(Instant.parse("2026-09-30T10:00:00Z"));
        jwtService = new JwtService(new JwtProperties(SECRET, ISSUER, EXPIRATION), clock);
        LoginThrottle loginThrottle = new LoginThrottle(
                new LoginThrottleProperties(MAX_FAILURES, WINDOW, LOCKOUT), clock);
        authenticationService = new AuthenticationService(userRepository, passwordEncoder,
                personProfiles, jwtService, loginThrottle);
    }

    @Test
    void it_issues_a_token_with_the_roles_and_their_permissions() {
        givenTheAccount("ana", Set.of(Role.ADMIN));

        LoginResponse session = authenticationService.login(new LoginRequest("ana", PASSWORD));

        Claims claims = jwtService.verify(session.accessToken()).getPayload();
        assertThat(claims.getSubject()).isEqualTo("ana");
        assertThat(jwtService.authoritiesOf(claims))
                .containsExactlyInAnyOrderElementsOf(Role.grantedAuthoritiesOf(Set.of(Role.ADMIN)));
    }

    @Test
    void a_person_with_two_profiles_logs_in_once_and_carries_both_roles() {
        givenTheAccount("ana", Set.of(Role.ADMIN, Role.TEACHER));

        LoginResponse session = authenticationService.login(new LoginRequest("ana", PASSWORD));

        assertThat(session.roles()).extracting(RoleAccess::role)
                .containsExactly(Role.ADMIN, Role.TEACHER);
        assertThat(session.roles().get(0).permissions()).contains("teachers:read")
                .doesNotContain("administrators:read");
        assertThat(session.roles().get(1).permissions()).contains("courses:create")
                .doesNotContain("teachers:read");
        assertThat(jwtService.authoritiesOf(jwtService.verify(session.accessToken()).getPayload()))
                .contains("ROLE_ADMIN", "ROLE_TEACHER");
    }

    @Test
    void the_session_reports_the_username_and_the_lifetime_of_the_token() {
        givenTheAccount("ana", Set.of(Role.TEACHER));

        LoginResponse session = authenticationService.login(new LoginRequest("ANA", PASSWORD));

        assertThat(session.username()).isEqualTo("ana");
        assertThat(session.tokenType()).isEqualTo("Bearer");
        assertThat(session.expiresIn()).isEqualTo(EXPIRATION.toSeconds());
    }

    @Test
    void the_session_says_whether_the_password_must_be_changed() {
        givenTheAccount("ana", Set.of(Role.STUDENT));

        assertThat(authenticationService.login(new LoginRequest("ana", PASSWORD))
                .mustChangePassword()).isTrue();
    }

    @Test
    void an_unknown_username_does_not_say_that_it_does_not_exist() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> authenticationService.login(new LoginRequest("fantasma", PASSWORD)));

        assertThat(error.getCode()).isEqualTo("USR_INVALID_CREDENTIALS");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void a_wrong_password_answers_the_same_as_an_unknown_username() {
        givenTheAccount("ana");

        ApiException error = assertThrows(ApiException.class,
                () -> authenticationService.login(new LoginRequest("ana", "otra-contrasena")));

        assertThat(error.getCode()).isEqualTo("USR_INVALID_CREDENTIALS");
    }

    @Test
    void an_account_without_any_active_profile_cannot_log_in() {
        givenTheAccount("ana", Set.of());

        ApiException error = assertThrows(ApiException.class,
                () -> authenticationService.login(new LoginRequest("ana", PASSWORD)));

        assertThat(error.getCode()).isEqualTo("USR_INACTIVE_ACCOUNT");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void too_many_wrong_passwords_lock_the_account_even_for_the_right_one() {
        givenTheAccount("ana");
        failTimes("ana", MAX_FAILURES);

        ApiException error = assertThrows(ApiException.class,
                () -> authenticationService.login(new LoginRequest("ana", PASSWORD)));

        assertThat(error.getCode()).isEqualTo("USR_TOO_MANY_LOGIN_ATTEMPTS");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void an_unknown_username_is_locked_the_same_as_an_existing_one() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());
        failTimes("fantasma", MAX_FAILURES);

        ApiException error = assertThrows(ApiException.class,
                () -> authenticationService.login(new LoginRequest("FANTASMA", PASSWORD)));

        assertThat(error.getCode()).isEqualTo("USR_TOO_MANY_LOGIN_ATTEMPTS");
    }

    @Test
    void the_lock_is_lifted_once_the_lockout_has_passed() {
        givenTheAccount("ana", Set.of(Role.STUDENT));
        failTimes("ana", MAX_FAILURES);

        clock.advance(LOCKOUT);

        assertThat(authenticationService.login(new LoginRequest("ana", PASSWORD)).username())
                .isEqualTo("ana");
    }

    @Test
    void failures_older_than_the_window_do_not_add_up() {
        givenTheAccount("ana", Set.of(Role.STUDENT));
        failTimes("ana", MAX_FAILURES - 1);

        clock.advance(WINDOW);
        failTimes("ana", MAX_FAILURES - 1);

        assertThat(authenticationService.login(new LoginRequest("ana", PASSWORD)).username())
                .isEqualTo("ana");
    }

    @Test
    void a_successful_login_clears_the_previous_failures() {
        givenTheAccount("ana", Set.of(Role.STUDENT));
        failTimes("ana", MAX_FAILURES - 1);
        authenticationService.login(new LoginRequest("ana", PASSWORD));

        failTimes("ana", MAX_FAILURES - 1);

        assertThat(authenticationService.login(new LoginRequest("ana", PASSWORD)).username())
                .isEqualTo("ana");
    }

    private void failTimes(String username, int times) {
        for (int attempt = 0; attempt < times; attempt++) {
            assertThrows(ApiException.class, () -> authenticationService.login(
                    new LoginRequest(username, "otra-contrasena")));
        }
    }

    private void givenTheAccount(String username) {
        User account = User.create(PERSON, username, passwordEncoder.encode(PASSWORD));
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(account));
    }

    private void givenTheAccount(String username, Set<Role> roles) {
        givenTheAccount(username);
        when(personProfiles.rolesOf(PERSON)).thenReturn(roles);
    }
}
