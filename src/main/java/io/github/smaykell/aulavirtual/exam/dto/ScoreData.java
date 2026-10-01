package io.github.smaykell.aulavirtual.exam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ScoreData(
        @NotNull(message = ExamConstraints.SCORE_REQUIRED)
        @PositiveOrZero(message = ExamConstraints.SCORE_NOT_NEGATIVE)
        BigDecimal points,

        @Size(max = ExamConstraints.FEEDBACK_MAX, message = ExamConstraints.FEEDBACK_TOO_LONG)
        String feedback) {
}
