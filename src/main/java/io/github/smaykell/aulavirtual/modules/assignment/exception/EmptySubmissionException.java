package io.github.smaykell.aulavirtual.modules.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class EmptySubmissionException extends ApiException {

    public EmptySubmissionException() {
        super(AssignmentError.EMPTY_SUBMISSION);
    }
}
