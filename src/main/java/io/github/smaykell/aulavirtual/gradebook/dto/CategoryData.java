package io.github.smaykell.aulavirtual.gradebook.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CategoryData(
        UUID id,

        @NotBlank(message = GradebookConstraints.CATEGORY_NAME_REQUIRED)
        @Size(max = GradebookConstraints.CATEGORY_NAME_MAX,
                message = GradebookConstraints.CATEGORY_NAME_TOO_LONG)
        String name,

        @NotNull(message = GradebookConstraints.WEIGHT_REQUIRED)
        @DecimalMin(value = "0", message = GradebookConstraints.WEIGHT_RANGE)
        @DecimalMax(value = GradebookConstraints.WEIGHT_MAX,
                message = GradebookConstraints.WEIGHT_RANGE)
        BigDecimal weight) {
}
