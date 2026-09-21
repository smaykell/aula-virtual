package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class EnrollmentNotPendingException extends ApiException {

    public EnrollmentNotPendingException() {
        super(CourseError.ENROLLMENT_NOT_PENDING);
    }
}
