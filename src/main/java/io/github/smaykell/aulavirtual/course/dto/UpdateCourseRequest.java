package io.github.smaykell.aulavirtual.course.dto;

import io.github.smaykell.aulavirtual.course.EnrollmentPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateCourseRequest(
        @NotBlank(message = CourseConstraints.NAME_REQUIRED)
        @Size(max = CourseConstraints.NAME_MAX, message = CourseConstraints.NAME_TOO_LONG)
        String name,

        @Size(max = CourseConstraints.DESCRIPTION_MAX,
                message = CourseConstraints.DESCRIPTION_TOO_LONG)
        String description,

        @NotNull(message = CourseConstraints.TEACHER_REQUIRED)
        UUID teacherId,

        @NotNull(message = CourseConstraints.POLICY_REQUIRED)
        EnrollmentPolicy enrollmentPolicy,

        @NotNull(message = CourseConstraints.START_DATE_REQUIRED)
        LocalDate startDate,

        @NotNull(message = CourseConstraints.END_DATE_REQUIRED)
        LocalDate endDate) {
}
