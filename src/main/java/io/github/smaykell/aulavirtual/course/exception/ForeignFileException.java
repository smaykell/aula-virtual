package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ForeignFileException extends ApiException {

    public ForeignFileException() {
        super(CourseError.FOREIGN_FILE);
    }
}
