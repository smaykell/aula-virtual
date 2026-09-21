package io.github.smaykell.aulavirtual.modules.course.dto;

import io.github.smaykell.aulavirtual.modules.course.Enrollment;
import io.github.smaykell.aulavirtual.modules.course.EnrollmentStatus;
import io.github.smaykell.aulavirtual.modules.student.dto.StudentSummary;
import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        CourseSummary course,
        StudentSummary student,
        EnrollmentStatus status,
        Instant requestedAt,
        Instant decidedAt) {

    public static EnrollmentResponse from(Enrollment enrollment, CourseSummary course,
            StudentSummary student) {

        return new EnrollmentResponse(enrollment.getId(), course, student,
                enrollment.getStatus(), enrollment.getRequestedAt(), enrollment.getDecidedAt());
    }
}
