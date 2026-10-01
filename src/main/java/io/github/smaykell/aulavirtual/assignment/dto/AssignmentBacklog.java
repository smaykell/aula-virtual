package io.github.smaykell.aulavirtual.assignment.dto;

import java.time.Instant;
import java.util.UUID;

public record AssignmentBacklog(
        UUID assignmentId,
        UUID courseId,
        String title,
        Instant dueAt,
        long awaiting) {
}
