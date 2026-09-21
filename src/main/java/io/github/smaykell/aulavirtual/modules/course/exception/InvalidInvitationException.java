package io.github.smaykell.aulavirtual.modules.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class InvalidInvitationException extends ApiException {

    public InvalidInvitationException() {
        super(CourseError.INVALID_INVITATION);
    }
}
