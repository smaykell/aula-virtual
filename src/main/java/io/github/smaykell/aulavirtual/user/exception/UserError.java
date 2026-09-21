package io.github.smaykell.aulavirtual.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserError implements ErrorCode {

    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "La persona no tiene una cuenta de acceso"),
    UNKNOWN_ACTOR(HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida. Vuelve a iniciar sesión."),
    INACTIVE_ACTOR(HttpStatus.FORBIDDEN,
            "Tu cuenta no tiene ningún perfil activo. Contacta al administrador."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"),
    INACTIVE_ACCOUNT(HttpStatus.FORBIDDEN,
            "Tu cuenta no tiene ningún perfil activo. Contacta al administrador."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta"),
    ROLE_OUT_OF_REACH(HttpStatus.FORBIDDEN,
            "No tienes permisos para administrar a personas con ese rol"),
    CREDENTIALS_REQUIRED(HttpStatus.BAD_REQUEST,
            "La persona aún no tiene cuenta: indica un usuario y una contraseña"),
    ACCOUNT_ALREADY_EXISTS(HttpStatus.CONFLICT,
            "La persona ya tiene una cuenta y seguirá usando la misma"),
    USERNAME_TAKEN(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre");

    public static final String PREFIX = "USR";

    private final HttpStatus status;
    private final String message;

    UserError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String prefix() {
        return PREFIX;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
