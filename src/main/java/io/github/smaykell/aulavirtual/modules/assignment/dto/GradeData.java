package io.github.smaykell.aulavirtual.modules.assignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GradeData(
        @NotNull(message = AssignmentConstraints.SCORE_REQUIRED)
        @PositiveOrZero(message = AssignmentConstraints.SCORE_NOT_NEGATIVE)
        BigDecimal score,

        @Size(max = AssignmentConstraints.INSTRUCTIONS_MAX,
                message = AssignmentConstraints.INSTRUCTIONS_TOO_LONG)
        String feedback) {
}
