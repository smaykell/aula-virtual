package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AssignmentError implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "Tarea con id %s no encontrada"),
    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Entrega con id %s no encontrada"),
    ONLY_STUDENTS_SUBMIT(HttpStatus.FORBIDDEN,
            "Solo un estudiante matriculado puede entregar una tarea"),
    SUBMISSION_OUT_OF_REACH(HttpStatus.FORBIDDEN, "Esa entrega no es tuya"),
    EMPTY_SUBMISSION(HttpStatus.BAD_REQUEST, "La entrega necesita un archivo o un texto"),
    DEADLINE_PASSED(HttpStatus.CONFLICT,
            "La fecha límite ya pasó y esta tarea no admite entregas tardías"),
    ALREADY_GRADED(HttpStatus.CONFLICT,
            "La entrega ya fue calificada y no admite cambios"),
    SCORE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "La nota debe estar entre 0 y %s");

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
