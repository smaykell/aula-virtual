package io.github.smaykell.aulavirtual.gradebook.dto;

import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import java.math.BigDecimal;
import java.util.UUID;

public record GradeEntry(
        GradeSource sourceType,
        UUID sourceId,
        UUID courseId,
        UUID studentId,
        BigDecimal maxScore,
        BigDecimal score,
        String feedback) {

    public boolean withinRange() {
        return score.signum() >= 0 && score.compareTo(maxScore) <= 0;
    }
}
