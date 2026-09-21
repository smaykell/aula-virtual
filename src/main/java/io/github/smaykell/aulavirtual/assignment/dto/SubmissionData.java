package io.github.smaykell.aulavirtual.assignment.dto;

import jakarta.validation.constraints.Size;

public record SubmissionData(
        @Size(max = AssignmentConstraints.STORAGE_KEY_MAX,
                message = AssignmentConstraints.STORAGE_KEY_TOO_LONG)
        String storageKey,

        @Size(max = AssignmentConstraints.TEXT_MAX,
                message = AssignmentConstraints.TEXT_TOO_LONG)
        String text) {
}
