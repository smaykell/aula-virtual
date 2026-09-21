package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AlreadyEnrolledException extends ApiException {

    public AlreadyEnrolledException() {
        super(CourseError.ALREADY_ENROLLED);
    }
}
