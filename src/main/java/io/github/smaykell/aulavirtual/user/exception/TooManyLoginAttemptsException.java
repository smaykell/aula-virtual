package io.github.smaykell.aulavirtual.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class TooManyLoginAttemptsException extends ApiException {

    public TooManyLoginAttemptsException() {
        super(UserError.TOO_MANY_LOGIN_ATTEMPTS);
    }
}
