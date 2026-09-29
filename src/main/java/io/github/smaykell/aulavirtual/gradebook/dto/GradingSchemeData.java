package io.github.smaykell.aulavirtual.gradebook.dto;

import io.github.smaykell.aulavirtual.gradebook.GradingMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record GradingSchemeData(
        @NotNull(message = GradebookConstraints.METHOD_REQUIRED)
        GradingMethod method,

        @NotNull(message = GradebookConstraints.PASSING_SCORE_REQUIRED)
        @DecimalMin(value = "0", inclusive = false,
                message = GradebookConstraints.PASSING_SCORE_RANGE)
        @DecimalMax(value = GradebookConstraints.SCALE_MAX,
                message = GradebookConstraints.PASSING_SCORE_RANGE)
        BigDecimal passingScore,

        @NotNull(message = GradebookConstraints.CATEGORIES_REQUIRED)
        @Size(max = GradebookConstraints.CATEGORIES_MAX,
                message = GradebookConstraints.CATEGORIES_TOO_MANY)
        List<@Valid CategoryData> categories) {
}
