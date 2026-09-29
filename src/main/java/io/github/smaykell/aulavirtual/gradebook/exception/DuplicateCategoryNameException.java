package io.github.smaykell.aulavirtual.gradebook.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class DuplicateCategoryNameException extends ApiException {

    public DuplicateCategoryNameException(String name) {
        super(GradebookError.DUPLICATE_CATEGORY_NAME, name);
    }
}
