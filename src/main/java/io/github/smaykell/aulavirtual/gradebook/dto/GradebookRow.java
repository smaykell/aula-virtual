package io.github.smaykell.aulavirtual.gradebook.dto;

import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.util.List;

public record GradebookRow(
        StudentSummary student,
        List<GradeResponse> grades,
        List<CategoryAverage> categories,
        FinalGrade finalGrade) {
}
