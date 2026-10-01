package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AttemptInProgressException extends ApiException {

    public AttemptInProgressException() {
        super(ExamError.ATTEMPT_IN_PROGRESS);
    }
}
