package io.github.smaykell.aulavirtual.modules.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException() {
        super(UserError.ACCOUNT_NOT_FOUND);
    }
}
