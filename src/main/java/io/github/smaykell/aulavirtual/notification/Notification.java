package io.github.smaykell.aulavirtual.notification;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "recipient", nullable = false, length = 160)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "body", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    private Notification(NotificationType type, String recipient, String subject, String body,
            Instant moment) {

        this.type = type;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = NotificationStatus.PENDING;
        this.availableAt = moment;
    }

    public static Notification pending(NotificationType type, String recipient, String subject,
            String body, Instant moment) {

        return new Notification(type, recipient, subject, body, moment);
    }

    void attempted(Instant nextTry) {
        this.attempts++;
        this.availableAt = nextTry;
    }

    void sent(Instant moment) {
        this.status = NotificationStatus.SENT;
        this.sentAt = moment;
        this.lastError = null;
    }

    void retryLater(String error) {
        this.lastError = error;
    }

    void givenUp(String error) {
        this.status = NotificationStatus.FAILED;
        this.lastError = error;
    }
}
