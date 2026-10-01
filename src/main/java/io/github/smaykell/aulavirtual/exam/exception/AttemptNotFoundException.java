package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class AttemptNotFoundException extends ApiException {

    public AttemptNotFoundException(UUID id) {
        super(ExamError.ATTEMPT_NOT_FOUND, id);
    }
}
