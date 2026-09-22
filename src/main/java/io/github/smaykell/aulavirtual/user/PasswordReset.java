package io.github.smaykell.aulavirtual.user;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "password_resets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordReset extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    private PasswordReset(UUID userId, String tokenHash, Instant requestedAt, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.requestedAt = requestedAt;
        this.expiresAt = expiresAt;
    }

    public static PasswordReset issue(UUID userId, String tokenHash, Instant requestedAt,
            Instant expiresAt) {

        return new PasswordReset(userId, tokenHash, requestedAt, expiresAt);
    }

    boolean isUsableAt(Instant moment) {
        return usedAt == null && moment.isBefore(expiresAt);
    }

    void use(Instant moment) {
        this.usedAt = moment;
    }
}
