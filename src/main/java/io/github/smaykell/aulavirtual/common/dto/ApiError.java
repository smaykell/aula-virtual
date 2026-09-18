package io.github.smaykell.aulavirtual.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        List<FieldIssue> errors) {

    public record FieldIssue(String field, String message) {
    }

    public static ApiError of(HttpStatusCode status, String message, String path) {
        return build(status, message, path, null);
    }

    public static ApiError ofValidation(String message, String path, List<FieldIssue> issues) {
        return build(HttpStatus.BAD_REQUEST, message, path, issues);
    }

    private static ApiError build(HttpStatusCode status, String message, String path,
            List<FieldIssue> issues) {
        return new ApiError(Instant.now(), status.value(), reasonPhraseOf(status), message, path,
                newTraceId(), issues);
    }

    private static String reasonPhraseOf(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : "Error";
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
