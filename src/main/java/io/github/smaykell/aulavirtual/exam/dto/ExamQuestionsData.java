package io.github.smaykell.aulavirtual.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ExamQuestionsData(
        @NotNull(message = ExamConstraints.QUESTIONS_REQUIRED)
        @Size(max = ExamConstraints.QUESTIONS_MAX, message = ExamConstraints.QUESTIONS_TOO_MANY)
        List<@Valid ExamQuestionData> questions) {
}
