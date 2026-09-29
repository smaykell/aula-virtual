package io.github.smaykell.aulavirtual.assignment.dto;

import io.github.smaykell.aulavirtual.assignment.Submission;
import io.github.smaykell.aulavirtual.assignment.SubmissionStatus;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        UUID assignmentId,
        StudentSummary student,
        String text,
        List<AttachmentResponse> attachments,
        Instant submittedAt,
        SubmissionStatus status,
        GradeResponse grade) {

    public static SubmissionResponse from(Submission submission, StudentSummary student,
            List<AttachmentResponse> attachments, GradeResponse grade) {

        return new SubmissionResponse(submission.getId(), submission.getAssignmentId(), student,
                submission.getText(), attachments, submission.getSubmittedAt(),
                submission.getStatus(), grade);
    }
}
