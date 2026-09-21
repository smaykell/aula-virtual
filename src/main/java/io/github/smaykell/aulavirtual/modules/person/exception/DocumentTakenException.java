package io.github.smaykell.aulavirtual.modules.person.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class DocumentTakenException extends ApiException {

    public DocumentTakenException() {
        super(PersonError.DOCUMENT_TAKEN);
    }
}
