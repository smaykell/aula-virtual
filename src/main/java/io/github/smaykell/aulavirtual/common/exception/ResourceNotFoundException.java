package io.github.smaykell.aulavirtual.common.exception;

import org.springframework.http.HttpStatus;

/** El recurso solicitado no existe. Se traduce a un 404. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }

    /** Por ejemplo: {@code new ResourceNotFoundException("Curso", id)}. */
    public ResourceNotFoundException(String recurso, Object id) {
        super(HttpStatus.NOT_FOUND, "%s con id %s no encontrado".formatted(recurso, id));
    }
}
