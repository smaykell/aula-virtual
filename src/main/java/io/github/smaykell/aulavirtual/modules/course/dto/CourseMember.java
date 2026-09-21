package io.github.smaykell.aulavirtual.modules.course.dto;

import java.util.UUID;

public record CourseMember(UUID courseId, boolean staff, UUID studentId) {
}
