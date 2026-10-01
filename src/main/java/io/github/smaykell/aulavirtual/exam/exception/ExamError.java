package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ExamError implements ErrorCode {

    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Pregunta con id %s no encontrada"),
    BANK_REQUIRES_STAFF(HttpStatus.FORBIDDEN,
            "Solo quien dicta o administra el curso puede ver el banco de preguntas"),
    CHOICES_OUT_OF_RANGE(HttpStatus.BAD_REQUEST,
            "Una pregunta de opciones necesita entre %s y %s opciones"),
    ONE_CORRECT_CHOICE(HttpStatus.BAD_REQUEST,
            "Una pregunta de opción única necesita exactamente una opción correcta"),
    SOME_CORRECT_CHOICE(HttpStatus.BAD_REQUEST,
            "Una pregunta de opción múltiple necesita al menos una opción correcta"),
    TRUTH_REQUIRED(HttpStatus.BAD_REQUEST, "Indica si el enunciado es verdadero o falso"),
    CHOICES_NOT_ALLOWED(HttpStatus.BAD_REQUEST,
            "Las preguntas de verdadero o falso y las de respuesta corta no llevan opciones");

    public static final String PREFIX = "EXM";

    private final HttpStatus status;
    private final String message;

    ExamError(HttpStatus status, String message) {
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
