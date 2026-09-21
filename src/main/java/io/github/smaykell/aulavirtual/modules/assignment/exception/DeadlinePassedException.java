package io.github.smaykell.aulavirtual.modules.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class DeadlinePassedException extends ApiException {

    public DeadlinePassedException() {
        super(AssignmentError.DEADLINE_PASSED);
    }
}
