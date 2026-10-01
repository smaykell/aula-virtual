package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ExamHasAttemptsException extends ApiException {

    public ExamHasAttemptsException() {
        super(ExamError.HAS_ATTEMPTS);
    }
}
