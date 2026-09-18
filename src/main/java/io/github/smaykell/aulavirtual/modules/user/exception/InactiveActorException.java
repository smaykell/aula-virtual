package io.github.smaykell.aulavirtual.modules.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class InactiveActorException extends ApiException {

    public InactiveActorException() {
        super(UserError.INACTIVE_ACTOR);
    }
}
