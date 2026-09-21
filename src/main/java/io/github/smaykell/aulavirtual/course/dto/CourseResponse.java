package io.github.smaykell.aulavirtual.course.dto;

import io.github.smaykell.aulavirtual.course.Course;
import io.github.smaykell.aulavirtual.course.CourseStatus;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentPolicy;
import io.github.smaykell.aulavirtual.teacher.dto.TeacherSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String name,
        String description,
        TeacherSummary teacher,
        InvitationResponse invitation,
        CourseStatus status,
        EnrollmentPolicy enrollmentPolicy,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt) {

    public static CourseResponse from(Course course, TeacherSummary teacher,
            InvitationResponse invitation) {

        return new CourseResponse(course.getId(), course.getName(), course.getDescription(),
                teacher, invitation, course.getStatus(), course.getEnrollmentPolicy(),
                course.getStartDate(), course.getEndDate(), course.getCreatedAt());
    }

    public static CourseResponse withoutInvitation(Course course, TeacherSummary teacher) {
        return from(course, teacher, null);
    }
}
