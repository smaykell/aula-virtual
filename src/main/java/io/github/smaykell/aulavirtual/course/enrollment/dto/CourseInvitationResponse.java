package io.github.smaykell.aulavirtual.course.enrollment.dto;

import io.github.smaykell.aulavirtual.settings.StudentIdentifier;

public record CourseInvitationResponse(
        String courseName,
        String teacherName,
        StudentIdentifier studentIdentifier,
        boolean open) {
}
