package io.github.smaykell.aulavirtual.modules.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class SubmissionNotFoundException extends ApiException {

    public SubmissionNotFoundException(UUID id) {
        super(AssignmentError.SUBMISSION_NOT_FOUND, id);
    }
}
