package io.github.smaykell.aulavirtual.modules.administrator.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AdministratorError implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "Administrador con id %s no encontrado"),
    ALREADY_REGISTERED(HttpStatus.CONFLICT, "Esa persona ya está registrada como administrador");

    public static final String PREFIX = "ADM";

    private final HttpStatus status;
    private final String message;

    AdministratorError(HttpStatus status, String message) {
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
