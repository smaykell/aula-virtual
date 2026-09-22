package io.github.smaykell.aulavirtual.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "indica tu usuario o tu correo")
        @Size(max = 160, message = "no puede superar los 160 caracteres")
        String identifier) {
}
