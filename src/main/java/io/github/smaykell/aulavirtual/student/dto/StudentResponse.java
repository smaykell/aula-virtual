package io.github.smaykell.aulavirtual.student.dto;

import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.student.Student;
import java.time.Instant;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        PersonResponse person,
        String username,
        String workplace,
        boolean active,
        Instant createdAt) {

    public static StudentResponse from(Student student, PersonResponse person, String username) {
        return new StudentResponse(student.getId(), person, username, student.getWorkplace(),
                student.isActive(), student.getCreatedAt());
    }
}
