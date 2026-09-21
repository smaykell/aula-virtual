package io.github.smaykell.aulavirtual.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class CurrentPasswordMismatchException extends ApiException {

    public CurrentPasswordMismatchException() {
        super(UserError.CURRENT_PASSWORD_MISMATCH);
    }
}
