package io.github.smaykell.aulavirtual.exam.question.dto;

import io.github.smaykell.aulavirtual.exam.question.QuestionOption;
import java.util.UUID;

public record OptionResponse(UUID id, String text, boolean correct) {

    public static OptionResponse from(QuestionOption option) {
        return new OptionResponse(option.getId(), option.getText(), option.isCorrect());
    }
}
