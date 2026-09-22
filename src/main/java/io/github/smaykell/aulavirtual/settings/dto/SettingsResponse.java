package io.github.smaykell.aulavirtual.settings.dto;

import io.github.smaykell.aulavirtual.settings.Settings;
import io.github.smaykell.aulavirtual.settings.StudentIdentifier;

public record SettingsResponse(
        StudentIdentifier studentIdentifier,
        boolean selfRegistrationEnabled) {

    public static SettingsResponse from(Settings settings) {
        return new SettingsResponse(settings.getStudentIdentifier(),
                settings.isSelfRegistrationEnabled());
    }
}
