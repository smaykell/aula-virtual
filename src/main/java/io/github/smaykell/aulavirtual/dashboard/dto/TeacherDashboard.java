package io.github.smaykell.aulavirtual.dashboard.dto;

import io.github.smaykell.aulavirtual.assignment.dto.AssignmentBacklog;
import io.github.smaykell.aulavirtual.course.dto.CourseCount;
import java.util.List;

public record TeacherDashboard(
        List<AssignmentBacklog> toReturn,
        List<CourseCount> pendingEnrollments) {
}
