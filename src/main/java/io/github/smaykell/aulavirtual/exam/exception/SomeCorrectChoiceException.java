package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class SomeCorrectChoiceException extends ApiException {

    public SomeCorrectChoiceException() {
        super(ExamError.SOME_CORRECT_CHOICE);
    }
}
