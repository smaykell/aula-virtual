package io.github.smaykell.aulavirtual.gradebook.dto;

import java.util.List;

public record GradebookResponse(
        GradingSchemeResponse scheme,
        List<GradeItem> items,
        List<GradebookRow> rows) {
}
