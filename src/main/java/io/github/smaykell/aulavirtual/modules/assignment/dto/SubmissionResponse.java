package io.github.smaykell.aulavirtual.modules.assignment.dto;

import io.github.smaykell.aulavirtual.modules.assignment.Submission;
import io.github.smaykell.aulavirtual.modules.assignment.SubmissionStatus;
import io.github.smaykell.aulavirtual.modules.student.dto.StudentSummary;
import java.time.Instant;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        UUID assignmentId,
        StudentSummary student,
        String storageKey,
        String text,
        Instant submittedAt,
        SubmissionStatus status,
        GradeResponse grade) {

    public static SubmissionResponse from(Submission submission, StudentSummary student,
            GradeResponse grade) {

        return new SubmissionResponse(submission.getId(), submission.getAssignmentId(), student,
                submission.getStorageKey(), submission.getText(), submission.getSubmittedAt(),
                submission.getStatus(), grade);
    }
}
