package io.github.smaykell.aulavirtual.person.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class PersonNotFoundException extends ApiException {

    public PersonNotFoundException(UUID id) {
        super(PersonError.NOT_FOUND, id);
    }
}
