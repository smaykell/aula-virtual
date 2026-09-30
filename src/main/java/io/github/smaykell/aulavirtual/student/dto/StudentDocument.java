package io.github.smaykell.aulavirtual.student.dto;

import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;

public record StudentDocument(DocumentType type, String number) {

    public static StudentDocument from(PersonResponse person) {
        return new StudentDocument(person.documentType(), person.documentNumber());
    }
}
