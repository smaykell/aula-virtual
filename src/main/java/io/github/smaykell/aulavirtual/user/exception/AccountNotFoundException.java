package io.github.smaykell.aulavirtual.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException() {
        super(UserError.ACCOUNT_NOT_FOUND);
    }
}
