package io.github.smaykell.aulavirtual.modules.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ArchivedCourseException extends ApiException {

    public ArchivedCourseException() {
        super(CourseError.ARCHIVED);
    }
}
