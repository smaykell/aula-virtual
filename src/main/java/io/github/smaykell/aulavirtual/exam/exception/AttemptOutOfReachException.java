package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AttemptOutOfReachException extends ApiException {

    public AttemptOutOfReachException() {
        super(ExamError.ATTEMPT_OUT_OF_REACH);
    }
}
