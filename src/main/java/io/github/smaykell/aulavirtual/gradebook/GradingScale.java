package io.github.smaykell.aulavirtual.gradebook;

import java.math.BigDecimal;

public final class GradingScale {

    public static final BigDecimal MAX = new BigDecimal("20");
    public static final BigDecimal DEFAULT_PASSING_SCORE = new BigDecimal("13.00");
    public static final BigDecimal FULL_WEIGHT = new BigDecimal("100");

    private GradingScale() {
    }
}
