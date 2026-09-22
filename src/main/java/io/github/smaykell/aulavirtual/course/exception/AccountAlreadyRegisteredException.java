package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AccountAlreadyRegisteredException extends ApiException {

    public AccountAlreadyRegisteredException() {
        super(CourseError.ACCOUNT_ALREADY_REGISTERED);
    }
}
