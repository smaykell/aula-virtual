package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AttemptClosedException extends ApiException {

    public AttemptClosedException() {
        super(ExamError.ATTEMPT_CLOSED);
    }
}
