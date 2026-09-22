package io.github.smaykell.aulavirtual.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");
    private static final Map<String, String> WELCOME =
            Map.of("firstName", "Ana Maria", "username", "45678912");

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void an_enqueued_notification_is_born_pending_and_available_right_away() {
        notificationService.enqueue(NotificationType.ACCOUNT_CREATED, "ana@escuela.pe", WELCOME);

        Notification stored = captureTheStoredOne();
        assertThat(stored.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(stored.getAvailableAt()).isEqualTo(NOW);
        assertThat(stored.getAttempts()).isZero();
        assertThat(stored.getSentAt()).isNull();
    }

    @Test
    void the_message_is_rendered_when_it_is_enqueued_and_not_when_it_is_sent() {
        notificationService.enqueue(NotificationType.ACCOUNT_CREATED, "ana@escuela.pe", WELCOME);

        Notification stored = captureTheStoredOne();
        assertThat(stored.getBody()).contains("Ana Maria").contains("45678912");
        assertThat(stored.getSubject()).isNotBlank();
    }

    @Test
    void a_student_without_email_does_not_leave_an_unsendable_row_behind() {
        notificationService.enqueue(NotificationType.ACCOUNT_CREATED, null, WELCOME);
        notificationService.enqueue(NotificationType.ACCOUNT_CREATED, "   ", WELCOME);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void the_recipient_is_stored_without_the_spaces_around_it() {
        notificationService.enqueue(NotificationType.ACCOUNT_CREATED, "  ana@escuela.pe  ",
                WELCOME);

        assertThat(captureTheStoredOne().getRecipient()).isEqualTo("ana@escuela.pe");
    }

    private Notification captureTheStoredOne() {
        ArgumentCaptor<Notification> stored = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(stored.capture());
        return stored.getValue();
    }
}
