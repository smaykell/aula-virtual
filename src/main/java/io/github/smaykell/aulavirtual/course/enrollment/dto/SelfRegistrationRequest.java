package io.github.smaykell.aulavirtual.course.enrollment.dto;

import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.student.dto.StudentConstraints;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SelfRegistrationRequest(
        @NotNull(message = "los datos de la persona son obligatorios")
        @Valid PersonData person,

        @NotBlank(message = StudentConstraints.WORKPLACE_REQUIRED)
        @Size(max = StudentConstraints.WORKPLACE_MAX,
                message = StudentConstraints.WORKPLACE_TOO_LONG)
        String workplace) {
}
