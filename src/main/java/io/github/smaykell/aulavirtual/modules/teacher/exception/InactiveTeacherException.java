package io.github.smaykell.aulavirtual.modules.teacher.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class InactiveTeacherException extends ApiException {

    public InactiveTeacherException(UUID id) {
        super(TeacherError.INACTIVE, id);
    }
}
