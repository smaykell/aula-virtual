package io.github.smaykell.aulavirtual.gradebook.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class RepeatedCategoryException extends ApiException {

    public RepeatedCategoryException(UUID categoryId) {
        super(GradebookError.REPEATED_CATEGORY, categoryId);
    }
}
