package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ExamWithoutQuestionsException extends ApiException {

    public ExamWithoutQuestionsException() {
        super(ExamError.WITHOUT_QUESTIONS);
    }
}
