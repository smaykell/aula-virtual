package io.github.smaykell.aulavirtual.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class RoleOutOfReachException extends ApiException {

    public RoleOutOfReachException() {
        super(UserError.ROLE_OUT_OF_REACH);
    }
}
