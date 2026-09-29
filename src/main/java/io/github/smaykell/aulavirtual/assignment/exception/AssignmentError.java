package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AssignmentError implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "Tarea con id %s no encontrada"),
    ONLY_STUDENTS_SUBMIT(HttpStatus.FORBIDDEN,
            "Solo un estudiante matriculado puede entregar una tarea"),
    EMPTY_SUBMISSION(HttpStatus.BAD_REQUEST, "La entrega necesita un archivo o un texto"),
    DEADLINE_PASSED(HttpStatus.CONFLICT,
            "La fecha límite ya pasó y esta tarea no admite entregas tardías"),
    ALREADY_GRADED(HttpStatus.CONFLICT,
            "La nota de esta entrega ya se devolvió y la entrega no admite cambios"),
    HAS_WORK(HttpStatus.CONFLICT,
            "La tarea ya tiene entregas o notas y no se puede eliminar");

    public static final String PREFIX = "ASG";

    private final HttpStatus status;
    private final String message;

    AssignmentError(HttpStatus status, String message) {
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
