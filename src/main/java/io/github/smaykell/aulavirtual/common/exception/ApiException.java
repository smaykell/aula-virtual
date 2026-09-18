package io.github.smaykell.aulavirtual.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public ApiException(ErrorCode errorCode, Object... args) {
        super(errorCode.format(args));
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode.format(args), cause);
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return errorCode.status();
    }

    public String getCode() {
        return errorCode.code();
    }
}
