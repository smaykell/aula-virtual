package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class FileTypeNotAllowedException extends ApiException {

    public FileTypeNotAllowedException(String contentType) {
        super(AssignmentError.FILE_TYPE_NOT_ALLOWED, contentType);
    }
}
