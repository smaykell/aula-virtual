package io.github.smaykell.aulavirtual.modules.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class InactiveAccountException extends ApiException {

    public InactiveAccountException() {
        super(UserError.INACTIVE_ACCOUNT);
    }
}
