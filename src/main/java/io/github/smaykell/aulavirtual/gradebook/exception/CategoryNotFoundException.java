package io.github.smaykell.aulavirtual.gradebook.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class CategoryNotFoundException extends ApiException {

    public CategoryNotFoundException(UUID categoryId) {
        super(GradebookError.CATEGORY_NOT_FOUND, categoryId);
    }
}
