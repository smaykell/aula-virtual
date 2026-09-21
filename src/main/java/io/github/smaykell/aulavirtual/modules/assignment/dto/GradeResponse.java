package io.github.smaykell.aulavirtual.modules.assignment.dto;

import io.github.smaykell.aulavirtual.modules.assignment.Grade;
import io.github.smaykell.aulavirtual.modules.assignment.GradeSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GradeResponse(
        UUID id,
        GradeSource sourceType,
        UUID sourceId,
        UUID studentId,
        UUID courseId,
        BigDecimal score,
        String feedback,
        Instant gradedAt) {

    public static GradeResponse from(Grade grade) {
        return new GradeResponse(grade.getId(), grade.getSourceType(), grade.getSourceId(),
                grade.getStudentId(), grade.getCourseId(), grade.getScore(), grade.getFeedback(),
                grade.getGradedAt());
    }
}
