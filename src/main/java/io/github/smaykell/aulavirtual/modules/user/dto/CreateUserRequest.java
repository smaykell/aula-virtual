package io.github.smaykell.aulavirtual.modules.user.dto;

import io.github.smaykell.aulavirtual.security.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "el usuario es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
                message = "debe tener entre 3 y 50 caracteres, sin espacios ni símbolos distintos de . _ -")
        String username,

        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres")
        String password,

        @NotNull(message = "el rol es obligatorio")
        Role role) {
}
