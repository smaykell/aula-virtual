package io.github.smaykell.aulavirtual.modules.person.dto;

import io.github.smaykell.aulavirtual.modules.person.DocumentType;
import io.github.smaykell.aulavirtual.modules.person.Person;
import io.github.smaykell.aulavirtual.modules.person.Sex;
import java.time.LocalDate;
import java.util.UUID;

public record PersonResponse(
        UUID id,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        LocalDate birthDate,
        Sex sex) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getDocumentType(),
                person.getDocumentNumber(), person.getFirstName(), person.getLastName(),
                person.getBirthDate(), person.getSex());
    }
}
