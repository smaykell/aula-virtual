package io.github.smaykell.aulavirtual.modules.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(UserError.INVALID_CREDENTIALS);
    }
}
