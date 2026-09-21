package io.github.smaykell.aulavirtual.modules.teacher.dto;

import io.github.smaykell.aulavirtual.modules.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.modules.teacher.Teacher;
import java.util.UUID;

public record TeacherSummary(
        UUID id,
        String firstName,
        String lastName,
        boolean active) {

    public static TeacherSummary from(Teacher teacher, PersonResponse person) {
        return new TeacherSummary(teacher.getId(), person.firstName(), person.lastName(),
                teacher.isActive());
    }
}
