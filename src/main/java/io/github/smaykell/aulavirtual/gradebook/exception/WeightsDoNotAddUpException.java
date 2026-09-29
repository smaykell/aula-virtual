package io.github.smaykell.aulavirtual.gradebook.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.math.BigDecimal;

public class WeightsDoNotAddUpException extends ApiException {

    public WeightsDoNotAddUpException(BigDecimal total) {
        super(GradebookError.WEIGHTS_DO_NOT_ADD_UP, total.stripTrailingZeros().toPlainString());
    }
}
