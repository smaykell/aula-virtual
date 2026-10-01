package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class InvalidExamWindowException extends ApiException {

    public InvalidExamWindowException() {
        super(ExamError.INVALID_WINDOW);
    }
}
