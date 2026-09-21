package io.github.smaykell.aulavirtual.course.enrollment.dto;

import io.github.smaykell.aulavirtual.course.dto.CourseSummary;
import io.github.smaykell.aulavirtual.course.enrollment.Enrollment;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentStatus;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
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
