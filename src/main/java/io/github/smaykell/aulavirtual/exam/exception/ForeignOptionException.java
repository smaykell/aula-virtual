package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ForeignOptionException extends ApiException {

    public ForeignOptionException() {
        super(ExamError.FOREIGN_OPTION);
    }
}
