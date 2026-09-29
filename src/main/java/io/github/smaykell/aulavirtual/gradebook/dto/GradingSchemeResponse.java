package io.github.smaykell.aulavirtual.gradebook.dto;

import io.github.smaykell.aulavirtual.gradebook.GradeCategory;
import io.github.smaykell.aulavirtual.gradebook.GradingMethod;
import io.github.smaykell.aulavirtual.gradebook.GradingScheme;
import java.math.BigDecimal;
import java.util.List;

public record GradingSchemeResponse(
        GradingMethod method,
        BigDecimal passingScore,
        List<GradeCategoryResponse> categories) {

    public static GradingSchemeResponse from(GradingScheme scheme,
            List<GradeCategory> categories) {

        return new GradingSchemeResponse(scheme.getMethod(), scheme.getPassingScore(),
                categories.stream().map(GradeCategoryResponse::from).toList());
    }
}
