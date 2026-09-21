package io.github.smaykell.aulavirtual.course.unit.dto;

import io.github.smaykell.aulavirtual.course.dto.CourseConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnitData(
        @NotBlank(message = CourseConstraints.TITLE_REQUIRED)
        @Size(max = CourseConstraints.NAME_MAX, message = CourseConstraints.NAME_TOO_LONG)
        String title) {
}
