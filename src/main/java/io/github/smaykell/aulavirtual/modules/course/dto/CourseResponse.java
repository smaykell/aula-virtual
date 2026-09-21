package io.github.smaykell.aulavirtual.modules.course.dto;

import io.github.smaykell.aulavirtual.modules.course.Course;
import io.github.smaykell.aulavirtual.modules.course.CourseStatus;
import io.github.smaykell.aulavirtual.modules.teacher.dto.TeacherSummary;
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
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt) {

    public static CourseResponse from(Course course, TeacherSummary teacher,
            InvitationResponse invitation) {

        return new CourseResponse(course.getId(), course.getName(), course.getDescription(),
                teacher, invitation, course.getStatus(), course.getStartDate(),
                course.getEndDate(), course.getCreatedAt());
    }
}
