package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ForeignFileException extends ApiException {

    public ForeignFileException() {
        super(AssignmentError.FOREIGN_FILE);
    }
}
