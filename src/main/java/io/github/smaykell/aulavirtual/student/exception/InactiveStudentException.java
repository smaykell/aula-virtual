package io.github.smaykell.aulavirtual.student.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class InactiveStudentException extends ApiException {

    public InactiveStudentException(UUID id) {
        super(StudentError.INACTIVE, id);
    }
}
