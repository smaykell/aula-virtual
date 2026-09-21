package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.math.BigDecimal;

public class ScoreOutOfRangeException extends ApiException {

    public ScoreOutOfRangeException(BigDecimal maxScore) {
        super(AssignmentError.SCORE_OUT_OF_RANGE, maxScore);
    }
}
