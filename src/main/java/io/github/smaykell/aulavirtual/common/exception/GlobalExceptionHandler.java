package io.github.smaykell.aulavirtual.common.exception;

import io.github.smaykell.aulavirtual.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce cualquier excepcion que escape de un controlador a un {@link ApiError}.
 *
 * <p>Regla de fondo: el cliente recibe un mensaje que le sirve para corregir su
 * peticion; los detalles internos van al log junto al mismo {@code traceId}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> manejarValidacionDeCuerpo(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ApiError.ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.ErrorCampo(error.getField(), mensajeDe(error)))
                .sorted(Comparator.comparing(ApiError.ErrorCampo::field))
                .toList();
        String traceId = nuevoTraceId();
        log.debug("[{}] Validacion fallida en {}: {}", traceId, request.getRequestURI(), errores);
        return ResponseEntity.badRequest()
                .body(ApiError.deValidacion("La peticion contiene campos invalidos",
                        request.getRequestURI(), traceId, errores));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> manejarValidacionDeParametros(HandlerMethodValidationException ex,
            HttpServletRequest request) {
        List<ApiError.ErrorCampo> errores = ex.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(error -> new ApiError.ErrorCampo(
                                resultado.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        String traceId = nuevoTraceId();
        log.debug("[{}] Parametros invalidos en {}: {}", traceId, request.getRequestURI(), errores);
        return ResponseEntity.badRequest()
                .body(ApiError.deValidacion("La peticion contiene parametros invalidos",
                        request.getRequestURI(), traceId, errores));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> manejarViolacionDeRestriccion(ConstraintViolationException ex,
            HttpServletRequest request) {
        List<ApiError.ErrorCampo> errores = ex.getConstraintViolations().stream()
                .map(violacion -> new ApiError.ErrorCampo(
                        violacion.getPropertyPath().toString(), violacion.getMessage()))
                .toList();
        String traceId = nuevoTraceId();
        log.debug("[{}] Restriccion violada en {}: {}", traceId, request.getRequestURI(), errores);
        return ResponseEntity.badRequest()
                .body(ApiError.deValidacion("La peticion contiene valores invalidos",
                        request.getRequestURI(), traceId, errores));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> manejarTipoIncorrecto(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String mensaje = "El parametro '%s' no tiene un formato valido".formatted(ex.getName());
        return respuesta(HttpStatus.BAD_REQUEST, mensaje, request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> manejarCuerpoIlegible(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es un JSON valido",
                request, null);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> manejarApiException(ApiException ex, HttpServletRequest request) {
        return respuesta(ex.getStatus(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> manejarAccesoDenegado(AccessDeniedException ex,
            HttpServletRequest request) {
        return respuesta(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta accion",
                request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> manejarIntegridad(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT,
                "La operacion viola una restriccion de integridad de los datos", request, ex);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> manejarInesperada(Exception ex, HttpServletRequest request) {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error inesperado. Reporta el traceId al administrador.",
                request, ex);
    }

    private ResponseEntity<ApiError> respuesta(HttpStatus status, String mensaje,
            HttpServletRequest request, Exception causa) {
        String traceId = nuevoTraceId();
        if (status.is5xxServerError()) {
            log.error("[{}] {} {} -> {}", traceId, request.getMethod(), request.getRequestURI(),
                    status, causa);
        } else if (causa != null) {
            log.warn("[{}] {} {} -> {}: {}", traceId, request.getMethod(), request.getRequestURI(),
                    status, causa.getMessage());
        } else {
            log.debug("[{}] {} {} -> {}: {}", traceId, request.getMethod(), request.getRequestURI(),
                    status, mensaje);
        }
        return ResponseEntity.status(status)
                .body(ApiError.de(status.value(), status.getReasonPhrase(), mensaje,
                        request.getRequestURI(), traceId));
    }

    private String mensajeDe(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "valor invalido";
    }

    private String nuevoTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
