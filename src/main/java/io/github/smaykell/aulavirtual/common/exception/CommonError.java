package io.github.smaykell.aulavirtual.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum CommonError implements ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "La petición contiene campos inválidos"),
    INVALID_PARAMETERS(HttpStatus.BAD_REQUEST, "La petición contiene parámetros inválidos"),
    INVALID_VALUES(HttpStatus.BAD_REQUEST, "La petición contiene valores inválidos"),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido"),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "Un valor de la petición no tiene un formato válido"),
    PARAMETER_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "El parámetro %s no tiene un formato válido"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "La petición no es válida"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Se requiere un token de acceso válido"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "El recurso solicitado no existe"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED,
            "El método HTTP no está permitido para este recurso"),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "No se puede responder en el formato solicitado"),
    CONTENT_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE,
            "La petición excede el tamaño máximo permitido"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "El tipo de contenido de la petición no está soportado"),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT,
            "La operación viola una restricción de integridad de los datos"),
    UNEXPECTED(HttpStatus.INTERNAL_SERVER_ERROR,
            "Ha ocurrido un error inesperado. Reporta el traceId al administrador.");

    public static final String PREFIX = "GEN";

    private final HttpStatus status;
    private final String message;

    CommonError(HttpStatus status, String message) {
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

    public static CommonError forStatus(HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return UNEXPECTED;
        }
        HttpStatus resolved = HttpStatus.resolve(status.value());
        if (resolved == null) {
            return INVALID_REQUEST;
        }
        return switch (resolved) {
            case NOT_FOUND -> RESOURCE_NOT_FOUND;
            case METHOD_NOT_ALLOWED -> CommonError.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> CommonError.UNSUPPORTED_MEDIA_TYPE;
            case NOT_ACCEPTABLE -> CommonError.NOT_ACCEPTABLE;
            case CONTENT_TOO_LARGE -> CommonError.CONTENT_TOO_LARGE;
            case UNAUTHORIZED -> UNAUTHENTICATED;
            case FORBIDDEN -> ACCESS_DENIED;
            default -> INVALID_REQUEST;
        };
    }
}
