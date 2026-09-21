package io.github.smaykell.aulavirtual.course.unit.dto;

import io.github.smaykell.aulavirtual.course.dto.CourseConstraints;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderUnitsRequest(
        @NotEmpty(message = CourseConstraints.UNITS_REQUIRED)
        List<UUID> unitIds) {
}
