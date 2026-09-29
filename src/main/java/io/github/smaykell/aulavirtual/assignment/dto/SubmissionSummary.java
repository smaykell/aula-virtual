package io.github.smaykell.aulavirtual.assignment.dto;

import io.github.smaykell.aulavirtual.assignment.Submission;
import io.github.smaykell.aulavirtual.assignment.SubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubmissionSummary(
        UUID id,
        SubmissionStatus status,
        String text,
        List<AttachmentResponse> attachments,
        Instant submittedAt) {

    public static SubmissionSummary from(Submission submission,
            List<AttachmentResponse> attachments) {

        return new SubmissionSummary(submission.getId(), submission.getStatus(),
                submission.getText(), attachments, submission.getSubmittedAt());
    }
}
