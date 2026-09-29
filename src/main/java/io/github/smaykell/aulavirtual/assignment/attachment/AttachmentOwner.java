package io.github.smaykell.aulavirtual.assignment.attachment;

import java.util.UUID;

public record AttachmentOwner(UUID assignmentId, UUID submissionId) {

    public static AttachmentOwner ofAssignment(UUID assignmentId) {
        return new AttachmentOwner(assignmentId, null);
    }

    public static AttachmentOwner ofSubmission(UUID submissionId) {
        return new AttachmentOwner(null, submissionId);
    }
}
