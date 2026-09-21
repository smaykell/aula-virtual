package io.github.smaykell.aulavirtual.modules.course.dto;

import io.github.smaykell.aulavirtual.modules.course.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record MaterialData(
        @NotBlank(message = CourseConstraints.TITLE_REQUIRED)
        @Size(max = CourseConstraints.NAME_MAX, message = CourseConstraints.NAME_TOO_LONG)
        String title,

        @NotNull(message = CourseConstraints.TYPE_REQUIRED)
        MaterialType type,

        @Size(max = CourseConstraints.STORAGE_KEY_MAX,
                message = CourseConstraints.STORAGE_KEY_TOO_LONG)
        String storageKey,

        @Size(max = CourseConstraints.EXTERNAL_URL_MAX,
                message = CourseConstraints.EXTERNAL_URL_TOO_LONG)
        String externalUrl,

        Instant publishedAt,

        @NotNull(message = CourseConstraints.VISIBILITY_REQUIRED)
        Boolean visible) {
}
