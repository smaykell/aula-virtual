package io.github.smaykell.aulavirtual.gradebook.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GradebookError implements ErrorCode {

    OWN_GRADE(HttpStatus.FORBIDDEN, "No puedes calificar tu propio trabajo"),
    SCORE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "La nota debe estar entre 0 y %s"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND,
            "La categoría con id %s no pertenece a este curso"),
    REPEATED_CATEGORY(HttpStatus.BAD_REQUEST, "La categoría con id %s aparece dos veces"),
    DUPLICATE_CATEGORY_NAME(HttpStatus.BAD_REQUEST, "Hay dos categorías llamadas «%s»"),
    WEIGHTS_DO_NOT_ADD_UP(HttpStatus.BAD_REQUEST,
            "Los pesos de las categorías deben sumar 100; ahora suman %s"),
    EXPORT_REQUIRES_STAFF(HttpStatus.FORBIDDEN,
            "Solo quien dicta o administra el curso puede exportar el registro de notas");

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
