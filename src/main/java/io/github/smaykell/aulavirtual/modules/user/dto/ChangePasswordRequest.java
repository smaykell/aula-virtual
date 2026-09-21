package io.github.smaykell.aulavirtual.modules.user.dto;

import io.github.smaykell.aulavirtual.modules.user.dto.CreateUserRequest.Credentials;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = Credentials.PASSWORD_REQUIRED)
        @Size(min = Credentials.PASSWORD_MIN, max = Credentials.PASSWORD_MAX,
                message = Credentials.PASSWORD_SHAPE)
        String password) {
}
