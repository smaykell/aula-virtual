package io.github.smaykell.aulavirtual.person.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class EmailTakenException extends ApiException {

    public EmailTakenException() {
        super(PersonError.EMAIL_TAKEN);
    }
}
