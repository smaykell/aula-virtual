package io.github.smaykell.aulavirtual.modules.course.dto;

import io.github.smaykell.aulavirtual.modules.course.Course;
import io.github.smaykell.aulavirtual.modules.course.CourseStatus;
import java.util.UUID;

public record CourseSummary(
        UUID id,
        String name,
        CourseStatus status) {

    public static CourseSummary from(Course course) {
        return new CourseSummary(course.getId(), course.getName(), course.getStatus());
    }
}
