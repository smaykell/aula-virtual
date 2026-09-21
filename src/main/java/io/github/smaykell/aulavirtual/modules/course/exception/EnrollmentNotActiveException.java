package io.github.smaykell.aulavirtual.modules.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class EnrollmentNotActiveException extends ApiException {

    public EnrollmentNotActiveException() {
        super(CourseError.ENROLLMENT_NOT_ACTIVE);
    }
}
