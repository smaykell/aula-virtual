package io.github.smaykell.aulavirtual.course.announcement.dto;

import io.github.smaykell.aulavirtual.course.dto.CourseConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementData(
        @NotBlank(message = CourseConstraints.TITLE_REQUIRED)
        @Size(max = CourseConstraints.NAME_MAX, message = CourseConstraints.NAME_TOO_LONG)
        String title,

        @NotBlank(message = CourseConstraints.ANNOUNCEMENT_BODY_REQUIRED)
        @Size(max = CourseConstraints.DESCRIPTION_MAX,
                message = CourseConstraints.DESCRIPTION_TOO_LONG)
        String body) {
}
