package io.github.smaykell.aulavirtual.modules.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class LinkMaterialWithoutUrlException extends ApiException {

    public LinkMaterialWithoutUrlException() {
        super(CourseError.MATERIAL_NEEDS_URL);
    }
}
