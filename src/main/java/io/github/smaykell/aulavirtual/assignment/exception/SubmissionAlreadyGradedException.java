package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class SubmissionAlreadyGradedException extends ApiException {

    public SubmissionAlreadyGradedException() {
        super(AssignmentError.ALREADY_GRADED);
    }
}
