package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.unit.MaterialType;

public class FileTypeNotAllowedException extends ApiException {

    public FileTypeNotAllowedException(MaterialType type, String contentType) {
        super(CourseError.FILE_TYPE_NOT_ALLOWED, type, contentType);
    }
}
