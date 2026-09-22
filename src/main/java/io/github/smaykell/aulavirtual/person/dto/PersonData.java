package io.github.smaykell.aulavirtual.person.dto;

import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.person.Sex;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Locale;

public record PersonData(
        @NotNull(message = PersonConstraints.DOCUMENT_TYPE_REQUIRED)
        DocumentType documentType,

        @NotBlank(message = PersonConstraints.DOCUMENT_NUMBER_REQUIRED)
        @Size(max = PersonConstraints.DOCUMENT_NUMBER_MAX,
                message = PersonConstraints.DOCUMENT_NUMBER_TOO_LONG)
        String documentNumber,

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

        @Email(message = PersonConstraints.EMAIL_MALFORMED)
        @Size(max = PersonConstraints.EMAIL_MAX, message = PersonConstraints.EMAIL_TOO_LONG)
        String email) {

    public String normalizedEmail() {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
