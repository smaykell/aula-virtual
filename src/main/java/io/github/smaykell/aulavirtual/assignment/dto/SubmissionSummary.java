package io.github.smaykell.aulavirtual.assignment.dto;

import io.github.smaykell.aulavirtual.assignment.Submission;
import io.github.smaykell.aulavirtual.assignment.SubmissionStatus;
import java.time.Instant;
import java.util.UUID;

public record SubmissionSummary(
        UUID id,
        SubmissionStatus status,
        String text,
        String storageKey,
        Instant submittedAt) {

    public static SubmissionSummary from(Submission submission) {
        return new SubmissionSummary(submission.getId(), submission.getStatus(),
                submission.getText(), submission.getStorageKey(), submission.getSubmittedAt());
    }
}
