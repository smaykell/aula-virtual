package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ChoicesOutOfRangeException extends ApiException {

    public ChoicesOutOfRangeException(int min, int max) {
        super(ExamError.CHOICES_OUT_OF_RANGE, min, max);
    }
}
