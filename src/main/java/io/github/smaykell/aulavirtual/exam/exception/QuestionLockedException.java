package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class QuestionLockedException extends ApiException {

    public QuestionLockedException() {
        super(ExamError.QUESTION_LOCKED);
    }
}
