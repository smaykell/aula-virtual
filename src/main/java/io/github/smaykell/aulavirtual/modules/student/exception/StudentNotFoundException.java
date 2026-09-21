package io.github.smaykell.aulavirtual.modules.student.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class StudentNotFoundException extends ApiException {

    public StudentNotFoundException(UUID id) {
        super(StudentError.NOT_FOUND, id);
    }
}
