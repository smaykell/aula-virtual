package io.github.smaykell.aulavirtual.modules.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException(UUID id) {
        super(UserError.NOT_FOUND, id);
    }
}
