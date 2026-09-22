package io.github.smaykell.aulavirtual.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.accounts")
public record AccountProperties(
        @NotBlank String passwordResetUrl,
        @NotNull Duration passwordResetLifetime,
        @NotNull Duration passwordResetCooldown) {
}
