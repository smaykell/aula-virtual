package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class RepeatedQuestionException extends ApiException {

    public RepeatedQuestionException(UUID id) {
        super(ExamError.REPEATED_QUESTION, id);
    }
}
