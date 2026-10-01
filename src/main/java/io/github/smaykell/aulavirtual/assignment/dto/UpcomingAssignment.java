package io.github.smaykell.aulavirtual.assignment.dto;

import io.github.smaykell.aulavirtual.assignment.Assignment;
import java.time.Instant;
import java.util.UUID;

public record UpcomingAssignment(UUID id, UUID courseId, String title, Instant dueAt) {

    public static UpcomingAssignment from(Assignment assignment) {
        return new UpcomingAssignment(assignment.getId(), assignment.getCourseId(),
                assignment.getTitle(), assignment.getDueAt());
    }
}
