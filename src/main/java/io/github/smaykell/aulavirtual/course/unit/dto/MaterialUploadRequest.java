package io.github.smaykell.aulavirtual.course.unit.dto;

import io.github.smaykell.aulavirtual.course.dto.CourseConstraints;
import io.github.smaykell.aulavirtual.course.unit.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MaterialUploadRequest(
        @NotNull(message = CourseConstraints.TYPE_REQUIRED)
        MaterialType type,

        @NotBlank(message = CourseConstraints.FILE_NAME_REQUIRED)
        @Size(max = CourseConstraints.FILE_NAME_MAX, message = CourseConstraints.FILE_NAME_TOO_LONG)
        String fileName,

        @NotBlank(message = CourseConstraints.CONTENT_TYPE_REQUIRED)
        @Size(max = CourseConstraints.CONTENT_TYPE_MAX,
                message = CourseConstraints.CONTENT_TYPE_TOO_LONG)
        String contentType,

        @NotNull(message = CourseConstraints.SIZE_REQUIRED)
        @Positive(message = CourseConstraints.SIZE_POSITIVE)
        Long size) {
}
