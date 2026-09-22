package io.github.smaykell.aulavirtual.student.dto;

import io.github.smaykell.aulavirtual.person.dto.PersonResponse;

public record StudentContact(String firstName, String email) {

    public static StudentContact from(PersonResponse person) {
        return new StudentContact(person.firstName(), person.email());
    }
}
