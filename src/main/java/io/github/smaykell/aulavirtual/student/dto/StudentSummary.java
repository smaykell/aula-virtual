package io.github.smaykell.aulavirtual.student.dto;

import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.student.Student;
import java.util.UUID;

public record StudentSummary(
        UUID id,
        String firstName,
        String lastName,
        String workplace,
        boolean active) {

    public static StudentSummary from(Student student, PersonResponse person) {
        return new StudentSummary(student.getId(), person.firstName(), person.lastName(),
                student.getWorkplace(), student.isActive());
    }
}
