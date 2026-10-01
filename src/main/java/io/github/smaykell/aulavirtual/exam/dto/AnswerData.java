package io.github.smaykell.aulavirtual.exam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AnswerData(
        @Size(max = ExamConstraints.CHOICES_MAX, message = ExamConstraints.CHOICES_TOO_MANY)
        List<@NotNull UUID> selectedOptionIds,

        @Size(max = ExamConstraints.ANSWER_TEXT_MAX,
                message = ExamConstraints.ANSWER_TEXT_TOO_LONG)
        String text) {

    public List<UUID> selectedOrNone() {
        return selectedOptionIds == null ? List.of() : selectedOptionIds;
    }
}
