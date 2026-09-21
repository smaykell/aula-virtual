package io.github.smaykell.aulavirtual.modules.administrator.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AdministratorAlreadyRegisteredException extends ApiException {

    public AdministratorAlreadyRegisteredException() {
        super(AdministratorError.ALREADY_REGISTERED);
    }
}
