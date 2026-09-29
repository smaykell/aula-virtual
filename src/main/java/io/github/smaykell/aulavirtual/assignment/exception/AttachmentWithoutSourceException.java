package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AttachmentWithoutSourceException extends ApiException {

    public AttachmentWithoutSourceException() {
        super(AssignmentError.ATTACHMENT_WITHOUT_SOURCE);
    }
}
