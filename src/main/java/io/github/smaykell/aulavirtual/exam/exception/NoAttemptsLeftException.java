package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class NoAttemptsLeftException extends ApiException {

    public NoAttemptsLeftException() {
        super(ExamError.NO_ATTEMPTS_LEFT);
    }
}
