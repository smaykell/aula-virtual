package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class StudentRequiredException extends ApiException {

    public StudentRequiredException() {
        super(CourseError.STUDENT_REQUIRED);
    }
}
