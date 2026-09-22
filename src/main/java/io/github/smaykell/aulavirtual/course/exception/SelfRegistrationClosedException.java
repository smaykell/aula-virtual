package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class SelfRegistrationClosedException extends ApiException {

    public SelfRegistrationClosedException() {
        super(CourseError.SELF_REGISTRATION_CLOSED);
    }
}
