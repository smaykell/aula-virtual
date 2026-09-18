package io.github.smaykell.aulavirtual.modules.user.dto;

import io.github.smaykell.aulavirtual.security.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = Credentials.USERNAME_REQUIRED)
        @Pattern(regexp = Credentials.USERNAME_PATTERN, message = Credentials.USERNAME_SHAPE)
        String username,

        @NotBlank(message = Credentials.PASSWORD_REQUIRED)
        @Size(min = Credentials.PASSWORD_MIN, max = Credentials.PASSWORD_MAX,
                message = Credentials.PASSWORD_SHAPE)
        String password,

        @NotNull(message = "el rol es obligatorio")
        Role role) {

    public static final class Credentials {

        public static final String USERNAME_PATTERN = "^[A-Za-z0-9._-]{3,50}$";
        public static final String USERNAME_REQUIRED = "el usuario es obligatorio";
        public static final String USERNAME_SHAPE =
                "debe tener entre 3 y 50 caracteres, sin espacios ni símbolos distintos de . _ -";
        public static final int PASSWORD_MIN = 8;
        public static final int PASSWORD_MAX = 72;
        public static final String PASSWORD_REQUIRED = "la contraseña es obligatoria";
        public static final String PASSWORD_SHAPE = "debe tener entre 8 y 72 caracteres";

        private Credentials() {
        }
    }
}
