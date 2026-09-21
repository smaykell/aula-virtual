package io.github.smaykell.aulavirtual.modules.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class EnrollmentPendingException extends ApiException {

    public EnrollmentPendingException() {
        super(CourseError.ENROLLMENT_PENDING);
    }
}
