package io.github.smaykell.aulavirtual.modules.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class MaterialNotFoundException extends ApiException {

    public MaterialNotFoundException(UUID id) {
        super(CourseError.MATERIAL_NOT_FOUND, id);
    }
}
