package io.github.smaykell.aulavirtual.person.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class DocumentNotFoundException extends ApiException {

    public DocumentNotFoundException() {
        super(PersonError.DOCUMENT_NOT_FOUND);
    }
}
