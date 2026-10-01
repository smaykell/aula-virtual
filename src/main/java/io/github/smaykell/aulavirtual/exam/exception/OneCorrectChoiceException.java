package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class OneCorrectChoiceException extends ApiException {

    public OneCorrectChoiceException() {
        super(ExamError.ONE_CORRECT_CHOICE);
    }
}
