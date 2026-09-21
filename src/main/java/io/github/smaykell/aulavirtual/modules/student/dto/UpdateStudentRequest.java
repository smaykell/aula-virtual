package io.github.smaykell.aulavirtual.modules.student.dto;

import io.github.smaykell.aulavirtual.modules.person.dto.PersonData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateStudentRequest(
        @NotNull(message = "los datos de la persona son obligatorios")
        @Valid PersonData person) {
}
