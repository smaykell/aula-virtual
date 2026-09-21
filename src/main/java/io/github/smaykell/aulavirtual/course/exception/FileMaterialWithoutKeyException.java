package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.MaterialType;

public class FileMaterialWithoutKeyException extends ApiException {

    public FileMaterialWithoutKeyException(MaterialType type) {
        super(CourseError.MATERIAL_NEEDS_FILE, type);
    }
}
