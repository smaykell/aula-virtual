package io.github.smaykell.aulavirtual.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class PasswordUnchangedException extends ApiException {

    public PasswordUnchangedException() {
        super(UserError.PASSWORD_UNCHANGED);
    }
}
