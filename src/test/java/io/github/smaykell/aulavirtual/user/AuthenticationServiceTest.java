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

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonProfiles personProfiles;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(new JwtProperties(SECRET, ISSUER, EXPIRATION),
                Clock.systemUTC());
        authenticationService = new AuthenticationService(userRepository, passwordEncoder,
                personProfiles, jwtService);
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
