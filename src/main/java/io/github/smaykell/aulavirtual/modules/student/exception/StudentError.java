package io.github.smaykell.aulavirtual.modules.student.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum StudentError implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "Estudiante con id %s no encontrado"),
    ALREADY_REGISTERED(HttpStatus.CONFLICT, "Esa persona ya está registrada como estudiante"),
    INACTIVE(HttpStatus.CONFLICT, "El estudiante con id %s está dado de baja");

    public static final String PREFIX = "STD";

    private final HttpStatus status;
    private final String message;

    StudentError(HttpStatus status, String message) {
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
