package io.github.smaykell.aulavirtual.modules.course.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinCourseRequest(
        @NotBlank(message = CourseConstraints.INVITATION_CODE_REQUIRED)
        String code) {
}
