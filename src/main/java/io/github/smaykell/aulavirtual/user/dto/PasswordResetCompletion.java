package io.github.smaykell.aulavirtual.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetCompletion(
        @NotBlank(message = "falta el código del enlace")
        String token,

        @NotBlank(message = CredentialConstraints.PASSWORD_REQUIRED)
        @Size(min = CredentialConstraints.PASSWORD_MIN, max = CredentialConstraints.PASSWORD_MAX,
                message = CredentialConstraints.PASSWORD_SHAPE)
        String newPassword) {
}
