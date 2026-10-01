package io.github.smaykell.aulavirtual.exam.question.dto;

import io.github.smaykell.aulavirtual.exam.question.Question;
import io.github.smaykell.aulavirtual.exam.question.QuestionOption;
import io.github.smaykell.aulavirtual.exam.question.QuestionType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        UUID courseId,
        QuestionType type,
        String statement,
        List<OptionResponse> options,
        String modelAnswer,
        Instant createdAt) {

    public static QuestionResponse from(Question question, List<QuestionOption> options) {
        return new QuestionResponse(question.getId(), question.getCourseId(), question.getType(),
                question.getStatement(), options.stream().map(OptionResponse::from).toList(),
                question.getModelAnswer(), question.getCreatedAt());
    }
}
