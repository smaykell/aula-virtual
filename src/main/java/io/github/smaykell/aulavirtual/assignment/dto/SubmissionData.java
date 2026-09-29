package io.github.smaykell.aulavirtual.assignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SubmissionData(
        @Size(max = AssignmentConstraints.TEXT_MAX,
                message = AssignmentConstraints.TEXT_TOO_LONG)
        String text,

        @Size(max = AssignmentConstraints.ATTACHMENTS_MAX,
                message = AssignmentConstraints.ATTACHMENTS_TOO_MANY)
        List<@Valid AttachmentData> attachments) {

    public List<AttachmentData> attachmentsOrNone() {
        return attachments == null ? List.of() : attachments;
    }

    public boolean isEmpty() {
        return (text == null || text.isBlank()) && attachmentsOrNone().isEmpty();
    }
}
