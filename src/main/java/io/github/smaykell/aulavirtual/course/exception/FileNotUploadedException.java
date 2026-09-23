package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class FileNotUploadedException extends ApiException {

    public FileNotUploadedException() {
        super(CourseError.FILE_NOT_UPLOADED);
    }
}
