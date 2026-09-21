package io.github.smaykell.aulavirtual.teacher.dto;

import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.user.dto.Credentials;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateTeacherRequest(
        @NotNull(message = "los datos de la persona son obligatorios")
        @Valid PersonData person,

        @Valid Credentials credentials) {
}
