package io.github.smaykell.aulavirtual.student.dto;

import io.github.smaykell.aulavirtual.person.dto.PersonData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(
        @NotNull(message = "los datos de la persona son obligatorios")
        @Valid PersonData person,

        @Size(max = StudentConstraints.WORKPLACE_MAX,
                message = StudentConstraints.WORKPLACE_TOO_LONG)
        String workplace) {
}
