package io.github.smaykell.aulavirtual.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notifications.enabled", matchIfMissing = true)
class NotificationDispatcher {

    private final NotificationOutbox outbox;
    private final NotificationMailer mailer;

    // Sin @Transactional a proposito: una transaccion alrededor del bucle retendria el
    // bloqueo de las filas durante todo el viaje SMTP, y el fallo de un correo desharia
    // el SENT de los que ya habian salido.
    @Scheduled(fixedDelayString = "${app.notifications.poll-interval}")
    void dispatch() {
        for (PendingNotification pending : outbox.claim()) {
            deliver(pending);
        }
    }

    private void deliver(PendingNotification pending) {
        try {
            mailer.send(pending.recipient(), pending.subject(), pending.body());
            outbox.sent(pending.id());
        } catch (RuntimeException failure) {
            log.warn("No se pudo enviar la notificacion {}: {}", pending.id(),
                    failure.getMessage());
            outbox.failed(pending.id(), failure.getMessage());
        }
    }
}
