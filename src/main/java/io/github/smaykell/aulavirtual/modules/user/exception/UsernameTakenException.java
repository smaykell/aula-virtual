package io.github.smaykell.aulavirtual.modules.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class UsernameTakenException extends ApiException {

    public UsernameTakenException() {
        super(UserError.USERNAME_TAKEN);
    }
}
