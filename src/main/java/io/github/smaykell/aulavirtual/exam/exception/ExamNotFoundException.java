package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class ExamNotFoundException extends ApiException {

    public ExamNotFoundException(UUID id) {
        super(ExamError.NOT_FOUND, id);
    }
}
