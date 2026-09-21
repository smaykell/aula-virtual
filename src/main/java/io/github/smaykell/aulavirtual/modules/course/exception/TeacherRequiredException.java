package io.github.smaykell.aulavirtual.modules.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class TeacherRequiredException extends ApiException {

    public TeacherRequiredException() {
        super(CourseError.TEACHER_REQUIRED);
    }
}
