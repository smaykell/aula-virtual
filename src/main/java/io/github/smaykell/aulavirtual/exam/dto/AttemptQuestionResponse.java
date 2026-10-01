package io.github.smaykell.aulavirtual.exam.dto;

import io.github.smaykell.aulavirtual.exam.question.QuestionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AttemptQuestionResponse(
        UUID questionId,
        BigDecimal points,
        QuestionType type,
        String statement,
        List<AttemptOptionResponse> options,
        List<UUID> selectedOptionIds,
        String text,
        BigDecimal awarded,
        String feedback,
        String modelAnswer) {
}
