package io.github.smaykell.aulavirtual.exam.question.dto;

import io.github.smaykell.aulavirtual.exam.dto.ExamConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OptionData(
        @NotBlank(message = ExamConstraints.OPTION_TEXT_REQUIRED)
        @Size(max = ExamConstraints.OPTION_TEXT_MAX,
                message = ExamConstraints.OPTION_TEXT_TOO_LONG)
        String text,

        @NotNull(message = ExamConstraints.OPTION_CORRECT_REQUIRED)
        Boolean correct) {
}
