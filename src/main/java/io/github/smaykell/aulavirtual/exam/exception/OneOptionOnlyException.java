package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class OneOptionOnlyException extends ApiException {

    public OneOptionOnlyException() {
        super(ExamError.ONE_OPTION_ONLY);
    }
}
