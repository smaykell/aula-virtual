package io.github.smaykell.aulavirtual.modules.student.dto;

import io.github.smaykell.aulavirtual.modules.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.modules.student.Student;
import java.time.Instant;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        PersonResponse person,
        String username,
        boolean active,
        Instant createdAt) {

    public static StudentResponse from(Student student, PersonResponse person, String username) {
        return new StudentResponse(student.getId(), person, username, student.isActive(),
                student.getCreatedAt());
    }
}
