package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AnswersRequireStaffException extends ApiException {

    public AnswersRequireStaffException() {
        super(ExamError.ANSWERS_REQUIRE_STAFF);
    }
}
