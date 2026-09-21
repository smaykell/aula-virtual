package io.github.smaykell.aulavirtual.administrator.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class AdministratorNotFoundException extends ApiException {

    public AdministratorNotFoundException(UUID id) {
        super(AdministratorError.NOT_FOUND, id);
    }
}
