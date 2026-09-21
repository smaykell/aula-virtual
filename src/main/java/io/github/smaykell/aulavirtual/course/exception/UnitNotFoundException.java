package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class UnitNotFoundException extends ApiException {

    public UnitNotFoundException(UUID id) {
        super(CourseError.UNIT_NOT_FOUND, id);
    }
}
