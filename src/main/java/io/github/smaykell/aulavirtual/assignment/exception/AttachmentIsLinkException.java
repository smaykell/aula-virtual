package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class AttachmentIsLinkException extends ApiException {

    public AttachmentIsLinkException() {
        super(AssignmentError.ATTACHMENT_IS_LINK);
    }
}
