package io.github.smaykell.aulavirtual.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record ExamData(
        @NotBlank(message = ExamConstraints.TITLE_REQUIRED)
        @Size(max = ExamConstraints.TITLE_MAX, message = ExamConstraints.TITLE_TOO_LONG)
        String title,

        @Size(max = ExamConstraints.INSTRUCTIONS_MAX,
                message = ExamConstraints.INSTRUCTIONS_TOO_LONG)
        String instructions,

        @NotNull(message = ExamConstraints.OPENS_AT_REQUIRED)
        Instant opensAt,

        @NotNull(message = ExamConstraints.CLOSES_AT_REQUIRED)
        Instant closesAt,

        @Positive(message = ExamConstraints.TIME_LIMIT_POSITIVE)
        @Max(value = ExamConstraints.TIME_LIMIT_MAX, message = ExamConstraints.TIME_LIMIT_TOO_LONG)
        Integer timeLimitMinutes,

        @NotNull(message = ExamConstraints.ATTEMPTS_REQUIRED)
        @Min(value = 1, message = ExamConstraints.ATTEMPTS_RANGE)
        @Max(value = ExamConstraints.ATTEMPTS_MAX, message = ExamConstraints.ATTEMPTS_RANGE)
        Integer maxAttempts,

        @NotNull(message = ExamConstraints.SHUFFLE_QUESTIONS_REQUIRED)
        Boolean shuffleQuestions,

        @NotNull(message = ExamConstraints.SHUFFLE_OPTIONS_REQUIRED)
        Boolean shuffleOptions,

        @NotNull(message = ExamConstraints.SHOWS_ANSWERS_REQUIRED)
        Boolean showsAnswers,

        UUID categoryId) {

    public boolean closesAfterOpening() {
        return closesAt.isAfter(opensAt);
    }
}
