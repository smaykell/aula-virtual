package io.github.smaykell.aulavirtual.assignment;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.assignments")
public record AssignmentProperties(
        @NotNull Duration reminderLead,
        @NotNull Duration reminderInterval) {
}
