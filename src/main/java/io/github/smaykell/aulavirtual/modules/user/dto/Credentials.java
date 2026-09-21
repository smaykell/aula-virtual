package io.github.smaykell.aulavirtual.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record Credentials(
        @NotBlank(message = CredentialConstraints.USERNAME_REQUIRED)
        @Pattern(regexp = CredentialConstraints.USERNAME_PATTERN,
                message = CredentialConstraints.USERNAME_SHAPE)
        String username,

        @NotBlank(message = CredentialConstraints.PASSWORD_REQUIRED)
        @Size(min = CredentialConstraints.PASSWORD_MIN, max = CredentialConstraints.PASSWORD_MAX,
                message = CredentialConstraints.PASSWORD_SHAPE)
        String password) {
}
