package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import java.util.UUID;

public class AnnouncementNotFoundException extends ApiException {

    public AnnouncementNotFoundException(UUID id) {
        super(CourseError.ANNOUNCEMENT_NOT_FOUND, id);
    }
}
