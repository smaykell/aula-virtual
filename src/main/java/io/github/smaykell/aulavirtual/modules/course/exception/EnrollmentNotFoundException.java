package io.github.smaykell.aulavirtual.modules.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class EnrollmentNotFoundException extends ApiException {

    public EnrollmentNotFoundException(UUID id) {
        super(CourseError.ENROLLMENT_NOT_FOUND, id);
    }
}
