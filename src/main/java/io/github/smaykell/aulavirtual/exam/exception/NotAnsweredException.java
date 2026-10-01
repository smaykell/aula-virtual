package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class NotAnsweredException extends ApiException {

    public NotAnsweredException() {
        super(ExamError.NOT_ANSWERED);
    }
}
