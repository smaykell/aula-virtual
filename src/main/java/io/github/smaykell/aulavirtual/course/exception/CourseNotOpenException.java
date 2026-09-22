package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class CourseNotOpenException extends ApiException {

    public CourseNotOpenException() {
        super(CourseError.COURSE_NOT_OPEN);
    }
}
