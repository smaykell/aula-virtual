package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class TitularCannotEnrollException extends ApiException {

    public TitularCannotEnrollException() {
        super(CourseError.TITULAR_CANNOT_ENROLL);
    }
}
