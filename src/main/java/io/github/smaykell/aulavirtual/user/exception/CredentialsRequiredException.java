package io.github.smaykell.aulavirtual.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class CredentialsRequiredException extends ApiException {

    public CredentialsRequiredException() {
        super(UserError.CREDENTIALS_REQUIRED);
    }
}
