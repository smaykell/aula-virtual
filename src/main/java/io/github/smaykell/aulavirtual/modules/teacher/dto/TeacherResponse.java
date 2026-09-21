package io.github.smaykell.aulavirtual.modules.teacher.dto;

import io.github.smaykell.aulavirtual.modules.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.modules.teacher.Teacher;
import java.time.Instant;
import java.util.UUID;

public record TeacherResponse(
        UUID id,
        PersonResponse person,
        String username,
        boolean active,
        Instant createdAt) {

    public static TeacherResponse from(Teacher teacher, PersonResponse person, String username) {
        return new TeacherResponse(teacher.getId(), person, username, teacher.isActive(),
                teacher.getCreatedAt());
    }
}
