package io.github.smaykell.aulavirtual.dashboard.dto;

import io.github.smaykell.aulavirtual.assignment.dto.UpcomingAssignment;
import io.github.smaykell.aulavirtual.course.announcement.dto.AnnouncementSummary;
import io.github.smaykell.aulavirtual.gradebook.dto.ReturnedGrade;
import java.util.List;

public record StudentDashboard(
        List<UpcomingAssignment> upcoming,
        List<ReturnedGrade> recentGrades,
        List<AnnouncementSummary> announcements) {
}
