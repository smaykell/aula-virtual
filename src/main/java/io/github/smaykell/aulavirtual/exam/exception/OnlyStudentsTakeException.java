package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class OnlyStudentsTakeException extends ApiException {

    public OnlyStudentsTakeException() {
        super(ExamError.ONLY_STUDENTS_TAKE);
    }
}
