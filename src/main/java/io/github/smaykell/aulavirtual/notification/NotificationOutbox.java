package io.github.smaykell.aulavirtual.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class NotificationOutbox {

    private static final int ERROR_MAX = 500;

    private final NotificationRepository notificationRepository;
    private final NotificationProperties properties;
    private final Clock clock;

    @Transactional
    List<PendingNotification> claim() {
        Instant now = clock.instant();
        List<Notification> claimed =
                notificationRepository.claimPending(now, properties.batchSize());

        claimed.forEach(notification -> notification
                .attempted(now.plus(properties.backoffAfter(notification.getAttempts() + 1))));
        return claimed.stream().map(PendingNotification::of).toList();
    }

    @Transactional
    void sent(UUID notificationId) {
        existing(notificationId).sent(clock.instant());
    }

    @Transactional
    void failed(UUID notificationId, String error) {
        Notification notification = existing(notificationId);
        if (notification.getAttempts() >= properties.maxAttempts()) {
            notification.givenUp(shortened(error));
            return;
        }
        notification.retryLater(shortened(error));
    }

    private Notification existing(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalStateException(
                        "La notificacion %s se reclamo y ya no esta".formatted(notificationId)));
    }

    // El mensaje se recorta aqui y no se confia a la columna: un UPDATE que desborda
    // last_error hace rollback, la fila vuelve a PENDING con los mismos intentos y se
    // reenvia para siempre.
    private static String shortened(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= ERROR_MAX ? error : error.substring(0, ERROR_MAX);
    }
}
