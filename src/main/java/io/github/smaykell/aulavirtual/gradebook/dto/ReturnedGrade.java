package io.github.smaykell.aulavirtual.gradebook.dto;

import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReturnedGrade(
        GradeSource sourceType,
        UUID sourceId,
        UUID courseId,
        String title,
        BigDecimal score,
        BigDecimal maxScore,
        Instant returnedAt) {
}
