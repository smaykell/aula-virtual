package io.github.smaykell.aulavirtual.notification;

import java.util.UUID;

record PendingNotification(UUID id, String recipient, String subject, String body) {

    static PendingNotification of(Notification notification) {
        return new PendingNotification(notification.getId(), notification.getRecipient(),
                notification.getSubject(), notification.getBody());
    }
}
