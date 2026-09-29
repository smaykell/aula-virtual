package io.github.smaykell.aulavirtual.gradebook.dto;

import io.github.smaykell.aulavirtual.gradebook.Grade;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
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
        BigDecimal maxScore,
        String feedback,
        Instant gradedAt,
        Instant returnedAt) {

    public static GradeResponse from(Grade grade) {
        return new GradeResponse(grade.getId(), grade.getSourceType(), grade.getSourceId(),
                grade.getStudentId(), grade.getCourseId(), grade.getScore(), grade.getMaxScore(),
                grade.getFeedback(), grade.getGradedAt(), grade.getReturnedAt());
    }
}
