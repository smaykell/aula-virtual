package io.github.smaykell.aulavirtual.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationOutbox outbox;

    @Mock
    private NotificationMailer mailer;

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher(outbox, mailer);
    }

    @Test
    void what_goes_out_is_marked_as_sent() {
        PendingNotification pending = aPendingOne("ana@escuela.pe");
        when(outbox.claim()).thenReturn(List.of(pending));

        dispatcher.dispatch();

        verify(mailer).send("ana@escuela.pe", "Asunto", "Cuerpo");
        verify(outbox).sent(pending.id());
    }

    @Test
    void a_failure_is_recorded_instead_of_stopping_the_round() {
        PendingNotification pending = aPendingOne("ana@escuela.pe");
        when(outbox.claim()).thenReturn(List.of(pending));
        doThrow(new MailSendException("el servidor no respondio"))
                .when(mailer).send(any(), any(), any());

        dispatcher.dispatch();

        verify(outbox).failed(eq(pending.id()), any());
        verify(outbox, never()).sent(any());
    }

    @Test
    void one_that_fails_does_not_undo_the_ones_that_already_went_out() {
        PendingNotification first = aPendingOne("ana@escuela.pe");
        PendingNotification second = aPendingOne("luis@escuela.pe");
        when(outbox.claim()).thenReturn(List.of(first, second));
        lenient().doThrow(new MailSendException("el servidor no respondio"))
                .when(mailer).send(eq("luis@escuela.pe"), any(), any());

        dispatcher.dispatch();

        verify(outbox).sent(first.id());
        verify(outbox).failed(eq(second.id()), any());
    }

    @Test
    void an_empty_round_does_not_touch_the_mailer() {
        when(outbox.claim()).thenReturn(List.of());

        dispatcher.dispatch();

        verify(mailer, never()).send(any(), any(), any());
    }

    private static PendingNotification aPendingOne(String recipient) {
        return new PendingNotification(UUID.randomUUID(), recipient, "Asunto", "Cuerpo");
    }
}
