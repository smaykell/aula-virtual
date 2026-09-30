package io.github.smaykell.aulavirtual.notification;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        @NotNull Duration pollInterval,
        @Min(1) int batchSize,
        @Min(1) int maxAttempts,
        @NotNull Duration retryDelay,
        @NotBlank String from,
        @NotNull ZoneId timeZone) {

    private static final int BACKOFF_CEILING = 6;

    public Duration backoffAfter(int attempts) {
        return retryDelay.multipliedBy(1L << Math.min(attempts - 1, BACKOFF_CEILING));
    }
}
