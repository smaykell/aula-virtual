package io.github.smaykell.aulavirtual.exam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record ExamQuestionData(
        @NotNull(message = ExamConstraints.QUESTION_REQUIRED)
        UUID questionId,

        @NotNull(message = ExamConstraints.POINTS_REQUIRED)
        @Positive(message = ExamConstraints.POINTS_POSITIVE)
        BigDecimal points) {
}
