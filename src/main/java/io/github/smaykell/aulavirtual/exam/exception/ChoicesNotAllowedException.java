package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ChoicesNotAllowedException extends ApiException {

    public ChoicesNotAllowedException() {
        super(ExamError.CHOICES_NOT_ALLOWED);
    }
}
