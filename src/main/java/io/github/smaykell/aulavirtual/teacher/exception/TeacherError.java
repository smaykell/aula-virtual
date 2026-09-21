package io.github.smaykell.aulavirtual.teacher.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum TeacherError implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "Docente con id %s no encontrado"),
    ALREADY_REGISTERED(HttpStatus.CONFLICT, "Esa persona ya está registrada como docente"),
    INACTIVE(HttpStatus.CONFLICT, "El docente con id %s está dado de baja");

    public static final String PREFIX = "TCH";

    private final HttpStatus status;
    private final String message;

    TeacherError(HttpStatus status, String message) {
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
