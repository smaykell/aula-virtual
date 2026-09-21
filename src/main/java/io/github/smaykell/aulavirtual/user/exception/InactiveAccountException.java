package io.github.smaykell.aulavirtual.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class InactiveAccountException extends ApiException {

    public InactiveAccountException() {
        super(UserError.INACTIVE_ACCOUNT);
    }
}
