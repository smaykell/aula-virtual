package io.github.smaykell.aulavirtual.settings.dto;

import io.github.smaykell.aulavirtual.settings.StudentIdentifier;
import jakarta.validation.constraints.NotNull;

public record UpdateSettingsRequest(
        @NotNull(message = "indica como se identifican los estudiantes")
        StudentIdentifier studentIdentifier,

        boolean selfRegistrationEnabled) {
}
