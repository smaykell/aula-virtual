package io.github.smaykell.aulavirtual.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class InvalidPasswordResetException extends ApiException {

    public InvalidPasswordResetException() {
        super(UserError.INVALID_PASSWORD_RESET);
    }
}
