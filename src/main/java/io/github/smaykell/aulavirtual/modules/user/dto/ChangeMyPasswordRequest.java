package io.github.smaykell.aulavirtual.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeMyPasswordRequest(
        @NotBlank(message = "la contraseña actual es obligatoria")
        String currentPassword,

        @NotBlank(message = CredentialConstraints.PASSWORD_REQUIRED)
        @Size(min = CredentialConstraints.PASSWORD_MIN, max = CredentialConstraints.PASSWORD_MAX,
                message = CredentialConstraints.PASSWORD_SHAPE)
        String newPassword) {
}
