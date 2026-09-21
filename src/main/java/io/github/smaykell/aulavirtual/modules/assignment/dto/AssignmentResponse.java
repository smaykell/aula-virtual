package io.github.smaykell.aulavirtual.modules.assignment.dto;

import io.github.smaykell.aulavirtual.modules.assignment.Assignment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID unitId,
        String title,
        String instructions,
        Instant dueAt,
        BigDecimal maxScore,
        boolean allowsLate,
        String attachmentKey,
        Instant createdAt) {

    public static AssignmentResponse from(Assignment assignment) {
        return new AssignmentResponse(assignment.getId(), assignment.getUnitId(),
                assignment.getTitle(), assignment.getInstructions(), assignment.getDueAt(),
                assignment.getMaxScore(), assignment.isAllowsLate(),
                assignment.getAttachmentKey(), assignment.getCreatedAt());
    }
}
