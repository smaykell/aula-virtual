package io.github.smaykell.aulavirtual.modules.person.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class InvalidDocumentNumberException extends ApiException {

    public InvalidDocumentNumberException() {
        super(PersonError.INVALID_DOCUMENT_NUMBER);
    }
}
