package io.github.smaykell.aulavirtual.modules.teacher.dto;

import io.github.smaykell.aulavirtual.common.domain.Sex;
import io.github.smaykell.aulavirtual.common.dto.PersonConstraints;
import io.github.smaykell.aulavirtual.modules.user.dto.CreateUserRequest.Credentials;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTeacherRequest(
        @NotBlank(message = PersonConstraints.FIRST_NAME_REQUIRED)
        @Size(max = PersonConstraints.NAME_MAX, message = PersonConstraints.NAME_TOO_LONG)
        String firstName,

        @NotBlank(message = PersonConstraints.LAST_NAME_REQUIRED)
        @Size(max = PersonConstraints.NAME_MAX, message = PersonConstraints.NAME_TOO_LONG)
        String lastName,

        @NotNull(message = PersonConstraints.BIRTH_DATE_REQUIRED)
        @Past(message = PersonConstraints.BIRTH_DATE_IN_THE_PAST)
        LocalDate birthDate,

        @NotNull(message = PersonConstraints.SEX_REQUIRED)
        Sex sex,

        @NotBlank(message = Credentials.USERNAME_REQUIRED)
        @Pattern(regexp = Credentials.USERNAME_PATTERN, message = Credentials.USERNAME_SHAPE)
        String username,

        @NotBlank(message = Credentials.PASSWORD_REQUIRED)
        @Size(min = Credentials.PASSWORD_MIN, max = Credentials.PASSWORD_MAX,
                message = Credentials.PASSWORD_SHAPE)
        String password) {
}
