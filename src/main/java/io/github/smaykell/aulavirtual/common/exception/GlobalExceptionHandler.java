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

    private static final String DEFAULT_FIELD_MESSAGE = "valor inválido";

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
        return validationResponse(CommonError.VALIDATION_FAILED, issues, request);
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
        return validationResponse(CommonError.INVALID_PARAMETERS, issues, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        return respond(CommonError.MALFORMED_JSON, request, ex);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return respond(CommonError.PARAMETER_TYPE_MISMATCH, request, ex, mismatch.getName());
        }
        return respond(CommonError.TYPE_MISMATCH, request, ex);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        CommonError error = CommonError.forStatus(status);
        return respond(status, error, error.message(), request, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex,
            WebRequest request) {

        List<FieldIssue> issues = ex.getConstraintViolations().stream()
                .map(violation -> new FieldIssue(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return validationResponse(CommonError.INVALID_VALUES, issues, request);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        return respond(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex,
            WebRequest request) {

        return respond(CommonError.DATA_INTEGRITY_VIOLATION, request, ex);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        return respond(CommonError.UNEXPECTED, request, ex);
    }

    private ResponseEntity<Object> validationResponse(ErrorCode code, List<FieldIssue> issues,
            WebRequest request) {

        ApiError error = ApiError.ofValidation(code, pathOf(request), issues);
        log.debug("[{}] {} -> 400 {}: {}", error.traceId(), describe(request), error.code(), issues);
        return ResponseEntity.badRequest().body(error);
    }

    private ResponseEntity<Object> respond(ErrorCode code, WebRequest request, Exception cause,
            Object... args) {

        return respond(code.status(), code, code.format(args), request, cause);
    }

    private ResponseEntity<Object> respond(HttpStatusCode status, ErrorCode code, String message,
            WebRequest request, Exception cause) {

        ApiError error = ApiError.of(status, code, message, pathOf(request));
        logAt(status, error, request, cause);
        return ResponseEntity.status(status).body(error);
    }

    private void logAt(HttpStatusCode status, ApiError error, WebRequest request, Exception cause) {
        if (status.is5xxServerError()) {
            log.error("[{}] {} -> {} {}", error.traceId(), describe(request), status.value(),
                    error.code(), cause);
        } else if (HttpStatus.CONFLICT.isSameCodeAs(status)) {
            log.warn("[{}] {} -> {} {}: {}", error.traceId(), describe(request), status.value(),
                    error.code(), cause.getMessage());
        } else {
            log.debug("[{}] {} -> {} {}: {}", error.traceId(), describe(request), status.value(),
                    error.code(), error.message());
        }
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
