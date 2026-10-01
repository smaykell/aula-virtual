package io.github.smaykell.aulavirtual.exam.dto;

import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.util.List;

public record ExamResultResponse(
        StudentSummary student,
        List<AttemptSummary> attempts,
        GradeResponse grade) {
}
