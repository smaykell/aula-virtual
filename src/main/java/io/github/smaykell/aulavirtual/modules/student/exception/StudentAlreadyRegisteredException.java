package io.github.smaykell.aulavirtual.modules.student.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class StudentAlreadyRegisteredException extends ApiException {

    public StudentAlreadyRegisteredException() {
        super(StudentError.ALREADY_REGISTERED);
    }
}
