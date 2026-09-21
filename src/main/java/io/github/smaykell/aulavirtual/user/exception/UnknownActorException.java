package io.github.smaykell.aulavirtual.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class UnknownActorException extends ApiException {

    public UnknownActorException() {
        super(UserError.UNKNOWN_ACTOR);
    }
}
