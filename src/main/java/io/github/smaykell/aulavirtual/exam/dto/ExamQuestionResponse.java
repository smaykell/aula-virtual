package io.github.smaykell.aulavirtual.exam.dto;

import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import java.math.BigDecimal;

public record ExamQuestionResponse(int position, BigDecimal points, QuestionResponse question) {
}
