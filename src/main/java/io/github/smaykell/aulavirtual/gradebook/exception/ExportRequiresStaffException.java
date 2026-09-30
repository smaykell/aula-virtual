package io.github.smaykell.aulavirtual.gradebook.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ExportRequiresStaffException extends ApiException {

    public ExportRequiresStaffException() {
        super(GradebookError.EXPORT_REQUIRES_STAFF);
    }
}
