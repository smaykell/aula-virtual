package io.github.smaykell.aulavirtual.modules.teacher.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class TeacherAlreadyRegisteredException extends ApiException {

    public TeacherAlreadyRegisteredException() {
        super(TeacherError.ALREADY_REGISTERED);
    }
}
