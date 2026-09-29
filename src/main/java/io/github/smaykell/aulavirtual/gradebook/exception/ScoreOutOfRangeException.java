package io.github.smaykell.aulavirtual.gradebook.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.math.BigDecimal;

public class ScoreOutOfRangeException extends ApiException {

    public ScoreOutOfRangeException(BigDecimal maxScore) {
        super(GradebookError.SCORE_OUT_OF_RANGE, maxScore);
    }
}
