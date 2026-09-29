package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class FileTooLargeException extends ApiException {

    public FileTooLargeException(long maxMegabytes) {
        super(AssignmentError.FILE_TOO_LARGE, maxMegabytes);
    }
}
