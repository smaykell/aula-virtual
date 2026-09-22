package io.github.smaykell.aulavirtual.course.enrollment.dto;

import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentStatus;

public record SelfRegistrationResponse(String username, EnrollmentStatus status) {
}
