package io.github.smaykell.aulavirtual.modules.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class OnlyStudentsSubmitException extends ApiException {

    public OnlyStudentsSubmitException() {
        super(AssignmentError.ONLY_STUDENTS_SUBMIT);
    }
}
