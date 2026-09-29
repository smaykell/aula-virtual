package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class FileNotUploadedException extends ApiException {

    public FileNotUploadedException() {
        super(AssignmentError.FILE_NOT_UPLOADED);
    }
}
