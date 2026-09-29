package io.github.smaykell.aulavirtual.gradebook.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record FinalGrade(BigDecimal score, int roundedScore, boolean passed) {

    public static FinalGrade of(BigDecimal exactScore, BigDecimal passingScore) {
        BigDecimal score = exactScore.setScale(2, RoundingMode.HALF_UP);
        int rounded = score.setScale(0, RoundingMode.HALF_UP).intValueExact();
        return new FinalGrade(score, rounded,
                BigDecimal.valueOf(rounded).compareTo(passingScore) >= 0);
    }
}
