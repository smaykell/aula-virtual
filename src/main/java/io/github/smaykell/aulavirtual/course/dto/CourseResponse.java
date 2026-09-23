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
        boolean staff,
        CourseStatus status,
        EnrollmentPolicy enrollmentPolicy,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt) {

    public static CourseResponse forStaff(Course course, TeacherSummary teacher,
            InvitationResponse invitation) {

        return of(course, teacher, invitation, true);
    }

    public static CourseResponse forStudent(Course course, TeacherSummary teacher) {
        return of(course, teacher, null, false);
    }

    private static CourseResponse of(Course course, TeacherSummary teacher,
            InvitationResponse invitation, boolean staff) {

        return new CourseResponse(course.getId(), course.getName(), course.getDescription(),
                teacher, invitation, staff, course.getStatus(), course.getEnrollmentPolicy(),
                course.getStartDate(), course.getEndDate(), course.getCreatedAt());
    }
}
