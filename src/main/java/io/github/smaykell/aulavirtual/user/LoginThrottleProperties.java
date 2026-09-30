package io.github.smaykell.aulavirtual.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.accounts.login-throttle")
public record LoginThrottleProperties(
        @Positive int maxFailures,
        @NotNull Duration window,
        @NotNull Duration lockout) {
}
