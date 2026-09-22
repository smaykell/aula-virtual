package io.github.smaykell.aulavirtual.person.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PersonError implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "Persona con id %s no encontrada"),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "No hay ninguna persona registrada con ese documento"),
    INVALID_DOCUMENT_NUMBER(HttpStatus.BAD_REQUEST,
            "El número de documento no corresponde al tipo de documento elegido"),
    DOCUMENT_TAKEN(HttpStatus.CONFLICT, "Ya existe otra persona con ese documento"),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST,
            "El correo es obligatorio para poder identificarte con el"),
    EMAIL_TAKEN(HttpStatus.CONFLICT, "Ya existe otra persona con ese correo");

    public static final String PREFIX = "PRS";

    private final HttpStatus status;
    private final String message;

    PersonError(HttpStatus status, String message) {
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
