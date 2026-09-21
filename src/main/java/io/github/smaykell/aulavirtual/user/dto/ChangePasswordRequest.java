package io.github.smaykell.aulavirtual.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = CredentialConstraints.PASSWORD_REQUIRED)
        @Size(min = CredentialConstraints.PASSWORD_MIN, max = CredentialConstraints.PASSWORD_MAX,
                message = CredentialConstraints.PASSWORD_SHAPE)
        String password) {
}
