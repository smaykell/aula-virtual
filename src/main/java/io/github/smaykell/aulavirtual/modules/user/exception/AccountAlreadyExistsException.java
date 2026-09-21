package io.github.smaykell.aulavirtual.modules.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AccountAlreadyExistsException extends ApiException {

    public AccountAlreadyExistsException() {
        super(UserError.ACCOUNT_ALREADY_EXISTS);
    }
}
