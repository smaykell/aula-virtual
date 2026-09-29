package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class StudentNotEnrolledException extends ApiException {

    public StudentNotEnrolledException() {
        super(CourseError.STUDENT_NOT_ENROLLED);
    }
}
