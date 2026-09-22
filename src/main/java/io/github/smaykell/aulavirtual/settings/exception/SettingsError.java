package io.github.smaykell.aulavirtual.settings.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum SettingsError implements ErrorCode {

    MANUAL_IDENTIFIER(HttpStatus.CONFLICT,
            "El centro asigna los usuarios manualmente: pide tu cuenta al administrador");

    public static final String PREFIX = "SET";

    private final HttpStatus status;
    private final String message;

    SettingsError(HttpStatus status, String message) {
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
