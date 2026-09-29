package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class AttachmentNotFoundException extends ApiException {

    public AttachmentNotFoundException(UUID attachmentId) {
        super(AssignmentError.ATTACHMENT_NOT_FOUND, attachmentId);
    }
}
