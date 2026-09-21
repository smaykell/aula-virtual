package io.github.smaykell.aulavirtual.modules.student.dto;

import io.github.smaykell.aulavirtual.modules.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.modules.student.Student;
import java.util.UUID;

public record StudentSummary(
        UUID id,
        String firstName,
        String lastName,
        boolean active) {

    public static StudentSummary from(Student student, PersonResponse person) {
        return new StudentSummary(student.getId(), person.firstName(), person.lastName(),
                student.isActive());
    }
}
