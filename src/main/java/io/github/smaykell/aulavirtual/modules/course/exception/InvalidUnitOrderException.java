package io.github.smaykell.aulavirtual.modules.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class InvalidUnitOrderException extends ApiException {

    public InvalidUnitOrderException() {
        super(CourseError.INVALID_UNIT_ORDER);
    }
}
