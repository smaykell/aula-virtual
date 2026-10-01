package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ExamNotOpenException extends ApiException {

    public ExamNotOpenException() {
        super(ExamError.NOT_OPEN);
    }
}
