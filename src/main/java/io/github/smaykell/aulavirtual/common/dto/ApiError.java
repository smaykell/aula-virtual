package io.github.smaykell.aulavirtual.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Cuerpo uniforme de todas las respuestas de error de la API.
 *
 * <p>El {@code traceId} tambien se escribe en el log del servidor, de modo que un
 * usuario puede reportar ese identificador y el error concreto se encuentra sin
 * exponer detalles internos en la respuesta.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        List<ErrorCampo> errors) {

    /** Error de validacion asociado a un campo concreto del cuerpo de la peticion. */
    public record ErrorCampo(String field, String message) {
    }

    public static ApiError de(int status, String error, String message, String path, String traceId) {
        return new ApiError(Instant.now(), status, error, message, path, traceId, null);
    }

    public static ApiError deValidacion(String message, String path, String traceId, List<ErrorCampo> errores) {
        return new ApiError(Instant.now(), 400, "Bad Request", message, path, traceId, errores);
    }
}
