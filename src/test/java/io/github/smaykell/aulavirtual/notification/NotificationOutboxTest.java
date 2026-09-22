package io.github.smaykell.aulavirtual.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");
    private static final NotificationProperties PROPERTIES = new NotificationProperties(
            Duration.ofSeconds(30), 50, 3, Duration.ofMinutes(1), "aula@escuela.pe");

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationOutbox outbox;

    @BeforeEach
    void setUp() {
        outbox = new NotificationOutbox(notificationRepository, PROPERTIES,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void claiming_counts_the_attempt_and_pushes_the_row_away_before_letting_go() {
        Notification pending = givenAPendingOne(UUID.randomUUID(), 0);
        when(notificationRepository.claimPending(NOW, 50)).thenReturn(List.of(pending));

        List<PendingNotification> claimed = outbox.claim();

        assertThat(claimed).hasSize(1);
        assertThat(pending.getAttempts()).isEqualTo(1);
        assertThat(pending.getAvailableAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
    }

    @Test
    void every_failed_attempt_waits_longer_than_the_one_before() {
        Notification pending = givenAPendingOne(UUID.randomUUID(), 2);
        when(notificationRepository.claimPending(NOW, 50)).thenReturn(List.of(pending));

        outbox.claim();

        assertThat(pending.getAvailableAt()).isEqualTo(NOW.plus(Duration.ofMinutes(4)));
    }

    @Test
    void a_sent_notification_forgets_the_error_that_it_had_before() {
        UUID id = UUID.randomUUID();
        Notification pending = givenAPendingOne(id, 1);
        pending.retryLater("el servidor no respondio");
        givenItIsFound(id, pending);

        outbox.sent(id);

        assertThat(pending.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(pending.getSentAt()).isEqualTo(NOW);
        assertThat(pending.getLastError()).isNull();
    }

    @Test
    void a_failure_below_the_limit_stays_pending_to_be_tried_again() {
        UUID id = UUID.randomUUID();
        Notification pending = givenAPendingOne(id, 1);
        givenItIsFound(id, pending);

        outbox.failed(id, "el servidor no respondio");

        assertThat(pending.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(pending.getLastError()).isEqualTo("el servidor no respondio");
    }

    @Test
    void a_failure_at_the_limit_gives_up_instead_of_trying_forever() {
        UUID id = UUID.randomUUID();
        Notification pending = givenAPendingOne(id, 3);
        givenItIsFound(id, pending);

        outbox.failed(id, "el servidor no respondio");

        assertThat(pending.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void a_very_long_error_is_cut_before_the_column_rejects_it() {
        UUID id = UUID.randomUUID();
        Notification pending = givenAPendingOne(id, 1);
        givenItIsFound(id, pending);

        outbox.failed(id, "x".repeat(5000));

        assertThat(pending.getLastError()).hasSize(500);
    }

    private void givenItIsFound(UUID id, Notification notification) {
        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));
    }

    private static Notification givenAPendingOne(UUID id, int attempts) {
        Notification notification = Notification.pending(NotificationType.ACCOUNT_CREATED,
                "ana@escuela.pe", "Asunto", "Cuerpo", NOW);
        ReflectionTestUtils.setField(notification, "id", id);
        ReflectionTestUtils.setField(notification, "attempts", attempts);
        return notification;
    }
}
