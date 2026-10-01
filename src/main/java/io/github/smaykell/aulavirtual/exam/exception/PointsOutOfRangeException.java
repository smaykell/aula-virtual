package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.math.BigDecimal;

public class PointsOutOfRangeException extends ApiException {

    public PointsOutOfRangeException(BigDecimal max) {
        super(ExamError.POINTS_OUT_OF_RANGE, max);
    }
}
