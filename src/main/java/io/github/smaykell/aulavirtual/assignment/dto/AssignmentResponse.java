package io.github.smaykell.aulavirtual.assignment.dto;

import io.github.smaykell.aulavirtual.assignment.Assignment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID unitId,
        UUID courseId,
        String title,
        String instructions,
        Instant dueAt,
        BigDecimal maxScore,
        boolean allowsLate,
        String attachmentKey,
        UUID categoryId,
        Instant createdAt) {

    public static AssignmentResponse from(Assignment assignment) {
        return new AssignmentResponse(assignment.getId(), assignment.getUnitId(),
                assignment.getCourseId(), assignment.getTitle(), assignment.getInstructions(), assignment.getDueAt(),
                assignment.getMaxScore(), assignment.isAllowsLate(),
                assignment.getAttachmentKey(), assignment.getCategoryId(),
                assignment.getCreatedAt());
    }
}
