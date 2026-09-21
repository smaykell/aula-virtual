package io.github.smaykell.aulavirtual.modules.administrator.dto;

import io.github.smaykell.aulavirtual.modules.person.dto.PersonData;
import io.github.smaykell.aulavirtual.modules.user.dto.Credentials;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateAdministratorRequest(
        @NotNull(message = "los datos de la persona son obligatorios")
        @Valid PersonData person,

        @Valid Credentials credentials) {
}
