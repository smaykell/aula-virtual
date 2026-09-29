package io.github.smaykell.aulavirtual.gradebook.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class OwnGradeException extends ApiException {

    public OwnGradeException() {
        super(GradebookError.OWN_GRADE);
    }
}
