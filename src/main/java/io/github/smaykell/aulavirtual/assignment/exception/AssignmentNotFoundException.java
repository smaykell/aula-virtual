package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class AssignmentNotFoundException extends ApiException {

    public AssignmentNotFoundException(UUID id) {
        super(AssignmentError.NOT_FOUND, id);
    }
}
