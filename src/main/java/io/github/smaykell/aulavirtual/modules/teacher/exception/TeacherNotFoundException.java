package io.github.smaykell.aulavirtual.modules.teacher.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class TeacherNotFoundException extends ApiException {

    public TeacherNotFoundException(UUID id) {
        super(TeacherError.NOT_FOUND, id);
    }
}
