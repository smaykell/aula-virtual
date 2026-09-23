package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.unit.MaterialType;

public class FileTooLargeException extends ApiException {

    public FileTooLargeException(MaterialType type) {
        super(CourseError.FILE_TOO_LARGE, type, type.maxMegabytes());
    }
}
