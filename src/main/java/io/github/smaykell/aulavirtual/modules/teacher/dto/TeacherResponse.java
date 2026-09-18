package io.github.smaykell.aulavirtual.modules.teacher.dto;

import io.github.smaykell.aulavirtual.common.domain.Sex;
import io.github.smaykell.aulavirtual.modules.teacher.Teacher;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TeacherResponse(
        UUID id,
        UUID userId,
        String username,
        String firstName,
        String lastName,
        LocalDate birthDate,
        Sex sex,
        boolean active,
        Instant createdAt) {

    public static TeacherResponse from(Teacher teacher, String username) {
        return new TeacherResponse(teacher.getId(), teacher.getUserId(), username,
                teacher.getFirstName(), teacher.getLastName(), teacher.getBirthDate(),
                teacher.getSex(), teacher.isActive(), teacher.getCreatedAt());
    }
}
