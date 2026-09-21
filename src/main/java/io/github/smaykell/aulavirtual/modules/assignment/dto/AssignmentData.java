package io.github.smaykell.aulavirtual.modules.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record AssignmentData(
        @NotBlank(message = AssignmentConstraints.TITLE_REQUIRED)
        @Size(max = AssignmentConstraints.TITLE_MAX,
                message = AssignmentConstraints.TITLE_TOO_LONG)
        String title,

        @Size(max = AssignmentConstraints.INSTRUCTIONS_MAX,
                message = AssignmentConstraints.INSTRUCTIONS_TOO_LONG)
        String instructions,

        @NotNull(message = AssignmentConstraints.DUE_AT_REQUIRED)
        Instant dueAt,

        @NotNull(message = AssignmentConstraints.MAX_SCORE_REQUIRED)
        @Positive(message = AssignmentConstraints.MAX_SCORE_POSITIVE)
        BigDecimal maxScore,

        @NotNull(message = AssignmentConstraints.LATE_REQUIRED)
        Boolean allowsLate,

        @Size(max = AssignmentConstraints.STORAGE_KEY_MAX,
                message = AssignmentConstraints.STORAGE_KEY_TOO_LONG)
        String attachmentKey) {
}
