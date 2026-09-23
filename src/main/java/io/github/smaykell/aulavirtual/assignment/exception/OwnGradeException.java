package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class OwnGradeException extends ApiException {

    public OwnGradeException() {
        super(AssignmentError.OWN_GRADE);
    }
}
