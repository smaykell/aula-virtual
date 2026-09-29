package io.github.smaykell.aulavirtual.gradebook.dto;

import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GradeItem(
        GradeSource sourceType,
        UUID sourceId,
        String title,
        UUID categoryId,
        BigDecimal maxScore,
        Instant dueAt) {
}
