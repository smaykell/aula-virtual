package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class TruthRequiredException extends ApiException {

    public TruthRequiredException() {
        super(ExamError.TRUTH_REQUIRED);
    }
}
