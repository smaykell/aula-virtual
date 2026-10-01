package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class QuestionNotFoundException extends ApiException {

    public QuestionNotFoundException(UUID id) {
        super(ExamError.QUESTION_NOT_FOUND, id);
    }
}
