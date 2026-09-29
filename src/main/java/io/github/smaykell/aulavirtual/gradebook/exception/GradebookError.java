package io.github.smaykell.aulavirtual.gradebook.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GradebookError implements ErrorCode {

    OWN_GRADE(HttpStatus.FORBIDDEN, "No puedes calificar tu propio trabajo"),
    SCORE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "La nota debe estar entre 0 y %s");

    public static final String PREFIX = "GRB";

    private final HttpStatus status;
    private final String message;

    GradebookError(HttpStatus status, String message) {
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
