package io.github.smaykell.aulavirtual.modules.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserError implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "Usuario con id %s no encontrado"),
    UNKNOWN_ACTOR(HttpStatus.UNAUTHORIZED,
            "Tu sesión ya no es válida. Vuelve a iniciar sesión."),
    INACTIVE_ACTOR(HttpStatus.FORBIDDEN, "Tu cuenta está desactivada. Contacta al administrador."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"),
    INACTIVE_ACCOUNT(HttpStatus.FORBIDDEN, "Tu cuenta está desactivada. Contacta al administrador."),
    ROLE_OUT_OF_REACH(HttpStatus.FORBIDDEN,
            "No tienes permisos para administrar usuarios con ese rol"),
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
