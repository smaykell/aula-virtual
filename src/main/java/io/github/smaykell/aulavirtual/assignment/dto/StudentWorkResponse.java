package io.github.smaykell.aulavirtual.assignment.dto;

import io.github.smaykell.aulavirtual.assignment.Submission;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.util.List;

public record StudentWorkResponse(
        StudentSummary student,
        SubmissionSummary submission,
        GradeResponse grade) {

    public static StudentWorkResponse of(StudentSummary student, Submission submission,
            List<AttachmentResponse> attachments, GradeResponse grade) {

        return new StudentWorkResponse(student,
                submission == null ? null : SubmissionSummary.from(submission, attachments),
                grade);
    }
}
