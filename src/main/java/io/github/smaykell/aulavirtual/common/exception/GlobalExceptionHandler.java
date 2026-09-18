package io.github.smaykell.aulavirtual.common.exception;

import io.github.smaykell.aulavirtual.common.dto.ApiError;
import io.github.smaykell.aulavirtual.common.dto.ApiError.FieldIssue;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String INVALID_BODY = "La peticion contiene campos invalidos";
    private static final String INVALID_PARAMETERS = "La peticion contiene parametros invalidos";
    private static final String INVALID_VALUES = "La peticion contiene valores invalidos";
    private static final String MALFORMED_JSON = "El cuerpo de la peticion no es un JSON valido";
    private static final String INVALID_REQUEST = "La peticion no es valida";
    private static final String INTEGRITY_CONFLICT =
            "La operacion viola una restriccion de integridad de los datos";
    private static final String UNEXPECTED =
            "Ha ocurrido un error inesperado. Reporta el traceId al administrador.";
    private static final String DEFAULT_FIELD_MESSAGE = "valor invalido";

    @ExceptionHandler({AuthenticationException.class, AccessDeniedException.class})
    void rethrowForSecurityFilterChain(RuntimeException ex) {
        throw ex;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldIssue(error.getField(), messageOf(error)))
                .sorted(Comparator.comparing(FieldIssue::field))
                .toList();
        return validationResponse(INVALID_BODY, issues, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {

        List<FieldIssue> issues = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldIssue(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return validationResponse(INVALID_PARAMETERS, issues, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        return respond(HttpStatus.BAD_REQUEST, MALFORMED_JSON, request, ex);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        return respond(HttpStatus.BAD_REQUEST, typeMismatchMessage(ex), request, ex);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        return respond(status, clientMessageFor(status), request, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex,
            WebRequest request) {

        List<FieldIssue> issues = ex.getConstraintViolations().stream()
                .map(violation -> new FieldIssue(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return validationResponse(INVALID_VALUES, issues, request);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        return respond(ex.getStatus(), ex.getMessage(), request, ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex,
            WebRequest request) {

        return respond(HttpStatus.CONFLICT, INTEGRITY_CONFLICT, request, ex);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED, request, ex);
    }

    private ResponseEntity<Object> validationResponse(String message, List<FieldIssue> issues,
            WebRequest request) {

        ApiError error = ApiError.ofValidation(message, pathOf(request), issues);
        log.debug("[{}] {} -> 400: {}", error.traceId(), describe(request), issues);
        return ResponseEntity.badRequest().body(error);
    }

    private ResponseEntity<Object> respond(HttpStatusCode status, String message,
            WebRequest request, Exception cause) {

        ApiError error = ApiError.of(status, message, pathOf(request));
        logAt(status, error, request, cause);
        return ResponseEntity.status(status).body(error);
    }

    private void logAt(HttpStatusCode status, ApiError error, WebRequest request, Exception cause) {
        if (status.is5xxServerError()) {
            log.error("[{}] {} -> {}", error.traceId(), describe(request), status.value(), cause);
        } else if (HttpStatus.CONFLICT.isSameCodeAs(status)) {
            log.warn("[{}] {} -> {}: {}", error.traceId(), describe(request), status.value(),
                    cause.getMessage());
        } else {
            log.debug("[{}] {} -> {}: {}", error.traceId(), describe(request), status.value(),
                    error.message());
        }
    }

    private String clientMessageFor(HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return UNEXPECTED;
        }
        HttpStatus resolved = HttpStatus.resolve(status.value());
        if (resolved == null) {
            return INVALID_REQUEST;
        }
        return switch (resolved) {
            case NOT_FOUND -> "El recurso solicitado no existe";
            case METHOD_NOT_ALLOWED -> "El metodo HTTP no esta permitido para este recurso";
            case UNSUPPORTED_MEDIA_TYPE -> "El tipo de contenido de la peticion no esta soportado";
            case NOT_ACCEPTABLE -> "No se puede responder en el formato solicitado";
            case PAYLOAD_TOO_LARGE -> "La peticion excede el tamano maximo permitido";
            default -> INVALID_REQUEST;
        };
    }

    private String typeMismatchMessage(TypeMismatchException ex) {
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return "El parametro %s no tiene un formato valido".formatted(mismatch.getName());
        }
        return "Un valor de la peticion no tiene un formato valido";
    }

    private String messageOf(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage()
                : DEFAULT_FIELD_MESSAGE;
    }

    private String pathOf(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : request.getDescription(false);
    }

    private String describe(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? "%s %s".formatted(servletRequest.getHttpMethod(),
                        servletRequest.getRequest().getRequestURI())
                : request.getDescription(false);
    }
}
