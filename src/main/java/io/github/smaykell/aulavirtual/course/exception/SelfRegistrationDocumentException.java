package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class SelfRegistrationDocumentException extends ApiException {

    public SelfRegistrationDocumentException() {
        super(CourseError.SELF_REGISTRATION_DOCUMENT);
    }
}
