package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class QuestionInUseException extends ApiException {

    public QuestionInUseException() {
        super(ExamError.QUESTION_IN_USE);
    }
}
