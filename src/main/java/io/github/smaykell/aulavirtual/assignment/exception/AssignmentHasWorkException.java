package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AssignmentHasWorkException extends ApiException {

    public AssignmentHasWorkException() {
        super(AssignmentError.HAS_WORK);
    }
}
