package io.github.smaykell.aulavirtual.student.dto;

import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.user.dto.Credentials;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateStudentRequest(
        @NotNull(message = "los datos de la persona son obligatorios")
        @Valid PersonData person,

        @Valid Credentials credentials) {
}
