package io.github.smaykell.aulavirtual.assignment.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record HandBackRequest(
        @NotEmpty(message = AssignmentConstraints.STUDENTS_REQUIRED)
        List<UUID> studentIds) {
}
