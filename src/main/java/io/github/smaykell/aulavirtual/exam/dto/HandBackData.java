package io.github.smaykell.aulavirtual.exam.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record HandBackData(
        @NotEmpty(message = ExamConstraints.STUDENTS_REQUIRED)
        List<UUID> studentIds) {
}
