package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class TitularIsEnrolledException extends ApiException {

    public TitularIsEnrolledException() {
        super(CourseError.TITULAR_IS_ENROLLED);
    }
}
