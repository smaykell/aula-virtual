package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class SubmissionOutOfReachException extends ApiException {

    public SubmissionOutOfReachException() {
        super(AssignmentError.SUBMISSION_OUT_OF_REACH);
    }
}
