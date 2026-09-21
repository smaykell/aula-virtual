package io.github.smaykell.aulavirtual.administrator.dto;

import io.github.smaykell.aulavirtual.person.dto.PersonData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateAdministratorRequest(
        @NotNull(message = "los datos de la persona son obligatorios")
        @Valid PersonData person) {
}
