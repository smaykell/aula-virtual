package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class CourseOutOfReachException extends ApiException {

    public CourseOutOfReachException() {
        super(CourseError.OUT_OF_REACH);
    }
}
