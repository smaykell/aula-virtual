package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ExamHasWorkException extends ApiException {

    public ExamHasWorkException() {
        super(ExamError.HAS_WORK);
    }
}
