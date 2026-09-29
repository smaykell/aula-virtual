package io.github.smaykell.aulavirtual.gradebook.dto;

import io.github.smaykell.aulavirtual.gradebook.GradeCategory;
import java.math.BigDecimal;
import java.util.UUID;

public record GradeCategoryResponse(UUID id, String name, BigDecimal weight, int position) {

    public static GradeCategoryResponse from(GradeCategory category) {
        return new GradeCategoryResponse(category.getId(), category.getName(),
                category.getWeight(), category.getPosition());
    }
}
