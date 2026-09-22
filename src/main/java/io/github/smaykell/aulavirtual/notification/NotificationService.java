package io.github.smaykell.aulavirtual.notification;

import java.time.Clock;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Transactional
    public void enqueue(NotificationType type, String recipient, Map<String, String> data) {
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        notificationRepository.save(Notification.pending(type, recipient.trim(),
                type.subjectFor(data), type.bodyFor(data), clock.instant()));
    }
}
