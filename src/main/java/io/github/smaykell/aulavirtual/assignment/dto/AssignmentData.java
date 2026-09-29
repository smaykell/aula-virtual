package io.github.smaykell.aulavirtual.assignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignmentData(
        @NotBlank(message = AssignmentConstraints.TITLE_REQUIRED)
        @Size(max = AssignmentConstraints.TITLE_MAX,
                message = AssignmentConstraints.TITLE_TOO_LONG)
        String title,

        @Size(max = AssignmentConstraints.INSTRUCTIONS_MAX,
                message = AssignmentConstraints.INSTRUCTIONS_TOO_LONG)
        String instructions,

        @NotNull(message = AssignmentConstraints.DUE_AT_REQUIRED)
        Instant dueAt,

        @NotNull(message = AssignmentConstraints.MAX_SCORE_REQUIRED)
        @Positive(message = AssignmentConstraints.MAX_SCORE_POSITIVE)
        BigDecimal maxScore,

        @NotNull(message = AssignmentConstraints.LATE_REQUIRED)
        Boolean allowsLate,

        @Size(max = AssignmentConstraints.ATTACHMENTS_MAX,
                message = AssignmentConstraints.ATTACHMENTS_TOO_MANY)
        List<@Valid AttachmentData> attachments,

        UUID categoryId) {

    public List<AttachmentData> attachmentsOrNone() {
        return attachments == null ? List.of() : attachments;
    }
}
