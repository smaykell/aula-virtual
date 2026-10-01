package io.github.smaykell.aulavirtual.dashboard.dto;

import java.util.Map;
import java.util.UUID;

public record DashboardResponse(
        Map<UUID, String> courses,
        StudentDashboard student,
        TeacherDashboard teacher) {
}
