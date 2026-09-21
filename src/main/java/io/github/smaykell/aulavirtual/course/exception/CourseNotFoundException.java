package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class CourseNotFoundException extends ApiException {

    public CourseNotFoundException(UUID id) {
        super(CourseError.NOT_FOUND, id);
    }
}
