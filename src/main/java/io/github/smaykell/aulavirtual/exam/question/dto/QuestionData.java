package io.github.smaykell.aulavirtual.exam.question.dto;

import io.github.smaykell.aulavirtual.exam.dto.ExamConstraints;
import io.github.smaykell.aulavirtual.exam.question.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuestionData(
        @NotNull(message = ExamConstraints.TYPE_REQUIRED)
        QuestionType type,

        @NotBlank(message = ExamConstraints.STATEMENT_REQUIRED)
        @Size(max = ExamConstraints.STATEMENT_MAX,
                message = ExamConstraints.STATEMENT_TOO_LONG)
        String statement,

        @Size(max = ExamConstraints.CHOICES_MAX, message = ExamConstraints.CHOICES_TOO_MANY)
        List<@Valid OptionData> options,

        Boolean statementIsTrue,

        @Size(max = ExamConstraints.MODEL_ANSWER_MAX,
                message = ExamConstraints.MODEL_ANSWER_TOO_LONG)
        String modelAnswer) {

    public List<OptionData> optionsOrNone() {
        return options == null ? List.of() : options;
    }
}
