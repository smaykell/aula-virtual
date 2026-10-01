package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class GradeReturnedException extends ApiException {

    public GradeReturnedException() {
        super(ExamError.GRADE_RETURNED);
    }
}
