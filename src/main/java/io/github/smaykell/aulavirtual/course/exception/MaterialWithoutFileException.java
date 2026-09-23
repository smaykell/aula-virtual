package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class MaterialWithoutFileException extends ApiException {

    public MaterialWithoutFileException() {
        super(CourseError.MATERIAL_WITHOUT_FILE);
    }
}
