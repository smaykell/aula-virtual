package io.github.smaykell.aulavirtual.modules.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class InvalidCourseDatesException extends ApiException {

    public InvalidCourseDatesException() {
        super(CourseError.INVALID_DATES);
    }
}
