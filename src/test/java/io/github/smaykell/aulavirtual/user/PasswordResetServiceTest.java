package io.github.smaykell.aulavirtual.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.Sex;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.user.dto.PasswordResetCompletion;
import io.github.smaykell.aulavirtual.user.dto.PasswordResetRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-22T10:00:00Z");
    private static final Duration LIFETIME = Duration.ofMinutes(30);
    private static final UUID PERSON = UUID.randomUUID();
    private static final UUID ACCOUNT = UUID.randomUUID();
    private static final String RESET_URL = "https://aula.example/reset-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private PersonService personService;

    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Map<String, String>> notificationData;

    private final PasswordResetTokens tokens = new PasswordResetTokens();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, passwordResetRepository,
                personService, notificationService, tokens, passwordEncoder,
                new AccountProperties(RESET_URL, LIFETIME, Duration.ofMinutes(2)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void the_link_goes_to_the_email_of_the_account_with_its_username() {
        givenTheAccount("12345678", "ana@example.com");

        service.request(new PasswordResetRequest(" 12345678 "));

        verify(notificationService).enqueue(eq(NotificationType.PASSWORD_RESET),
                eq("ana@example.com"), notificationData.capture());
        assertThat(notificationData.getValue())
                .containsEntry("username", "12345678")
                .containsEntry("minutes", "30");
        assertThat(notificationData.getValue().get("link")).startsWith(RESET_URL + "?token=");
    }

    @Test
    void the_account_can_also_be_found_by_its_email() {
        when(userRepository.findByUsername("ana@example.com")).thenReturn(Optional.empty());
        when(personService.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(person("ana@example.com")));
        when(userRepository.findByPersonId(PERSON)).thenReturn(Optional.of(account("12345678")));
        when(personService.get(PERSON)).thenReturn(person("ana@example.com"));

        service.request(new PasswordResetRequest("Ana@Example.com"));

        verify(notificationService).enqueue(eq(NotificationType.PASSWORD_RESET),
                eq("ana@example.com"), anyMap());
    }

    @Test
    void only_the_hash_of_the_token_is_stored() {
        givenTheAccount("12345678", "ana@example.com");

        service.request(new PasswordResetRequest("12345678"));

        ArgumentCaptor<PasswordReset> stored = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository).save(stored.capture());
        verify(notificationService).enqueue(any(), anyString(), notificationData.capture());
        String token = notificationData.getValue().get("link").substring(
                (RESET_URL + "?token=").length());
        assertThat(stored.getValue().getTokenHash())
                .isEqualTo(tokens.hashOf(token))
                .isNotEqualTo(token);
        assertThat(stored.getValue().getExpiresAt()).isEqualTo(NOW.plus(LIFETIME));
    }

    @Test
    void an_unknown_identifier_is_answered_in_silence() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());
        when(personService.findByEmail("nadie")).thenReturn(Optional.empty());

        service.request(new PasswordResetRequest("nadie"));

        verify(notificationService, never()).enqueue(any(), any(), any());
    }

    @Test
    void a_second_request_right_after_the_first_does_not_flood_the_inbox() {
        when(userRepository.findByUsername("12345678"))
                .thenReturn(Optional.of(account("12345678")));
        when(passwordResetRepository.existsByUserIdAndRequestedAtAfter(ACCOUNT,
                NOW.minus(Duration.ofMinutes(2)))).thenReturn(true);

        service.request(new PasswordResetRequest("12345678"));

        verify(notificationService, never()).enqueue(any(), any(), any());
    }

    @Test
    void without_an_email_there_is_nowhere_to_send_the_link() {
        givenTheAccount("12345678", null);

        service.request(new PasswordResetRequest("12345678"));

        verify(passwordResetRepository, never()).save(any());
        verify(notificationService, never()).enqueue(any(), any(), any());
    }

    @Test
    void a_valid_link_changes_the_password_and_closes_every_open_link() {
        User account = account("12345678");
        PasswordReset reset = givenTheReset("token-bueno", NOW.plusSeconds(60));
        PasswordReset older = PasswordReset.issue(ACCOUNT, "otro", NOW, NOW.plusSeconds(60));
        when(userRepository.findById(ACCOUNT)).thenReturn(Optional.of(account));
        when(passwordResetRepository.findByUserIdAndUsedAtIsNull(ACCOUNT))
                .thenReturn(List.of(reset, older));

        service.complete(new PasswordResetCompletion("token-bueno", "nueva-clave"));

        assertThat(passwordEncoder.matches("nueva-clave", account.getPasswordHash())).isTrue();
        assertThat(reset.getUsedAt()).isEqualTo(NOW);
        assertThat(older.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    void an_expired_link_is_rejected() {
        givenTheReset("token-viejo", NOW);

        ApiException error = assertThrows(ApiException.class, () ->
                service.complete(new PasswordResetCompletion("token-viejo", "nueva-clave")));

        assertThat(error.getCode()).isEqualTo("USR_INVALID_PASSWORD_RESET");
    }

    @Test
    void a_used_link_is_rejected() {
        PasswordReset reset = givenTheReset("token-usado", NOW.plusSeconds(60));
        reset.use(NOW.minusSeconds(10));

        ApiException error = assertThrows(ApiException.class, () ->
                service.complete(new PasswordResetCompletion("token-usado", "nueva-clave")));

        assertThat(error.getCode()).isEqualTo("USR_INVALID_PASSWORD_RESET");
    }

    @Test
    void a_made_up_link_is_rejected() {
        when(passwordResetRepository.findByTokenHash(tokens.hashOf("inventado")))
                .thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class, () ->
                service.complete(new PasswordResetCompletion("inventado", "nueva-clave")));

        assertThat(error.getCode()).isEqualTo("USR_INVALID_PASSWORD_RESET");
    }

    private User givenTheAccount(String username, String email) {
        User account = account(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(account));
        when(personService.get(PERSON)).thenReturn(person(email));
        return account;
    }

    private PasswordReset givenTheReset(String token, Instant expiresAt) {
        PasswordReset reset = PasswordReset.issue(ACCOUNT, tokens.hashOf(token),
                NOW.minusSeconds(600), expiresAt);
        when(passwordResetRepository.findByTokenHash(tokens.hashOf(token)))
                .thenReturn(Optional.of(reset));
        return reset;
    }

    private static User account(String username) {
        User account = User.create(PERSON, username, "hash");
        ReflectionTestUtils.setField(account, "id", ACCOUNT);
        return account;
    }

    private static PersonResponse person(String email) {
        return new PersonResponse(PERSON, DocumentType.DNI, "12345678", "Ana", "Lopez",
                LocalDate.of(1970, 1, 1), Sex.FEMALE, email);
    }
}
