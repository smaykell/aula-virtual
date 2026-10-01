package io.github.smaykell.aulavirtual.dashboard;

import io.github.smaykell.aulavirtual.assignment.AssignmentService;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.dashboard.dto.DashboardResponse;
import io.github.smaykell.aulavirtual.dashboard.dto.StudentDashboard;
import io.github.smaykell.aulavirtual.dashboard.dto.TeacherDashboard;
import io.github.smaykell.aulavirtual.gradebook.GradebookService;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Duration RECENT = Duration.ofDays(14);
    private static final int PENDING_LIMIT = 10;
    private static final int GRADES_LIMIT = 5;
    private static final int ANNOUNCEMENTS_LIMIT = 5;

    private final UserService userService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final GradebookService gradebookService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse of(String actorUsername) {
        Actor actor = userService.actor(actorUsername);
        Map<UUID, String> attended = actor.profileId(Role.STUDENT)
                .map(courseService::openCoursesAttendedBy)
                .orElse(Map.of());
        Map<UUID, String> taught = actor.profileId(Role.TEACHER)
                .map(courseService::openCoursesTaughtBy)
                .orElse(Map.of());

        Map<UUID, String> courses = new HashMap<>(attended);
        courses.putAll(taught);
        return new DashboardResponse(courses,
                actor.profileId(Role.STUDENT).map(id -> studentPart(id, attended)).orElse(null),
                actor.roles().contains(Role.TEACHER) ? teacherPart(taught) : null);
    }

    private StudentDashboard studentPart(UUID studentId, Map<UUID, String> courses) {
        Instant now = clock.instant();
        Instant recently = now.minus(RECENT);
        return new StudentDashboard(
                assignmentService.pendingFor(studentId, courses.keySet(), now, recently,
                        PENDING_LIMIT),
                gradebookService.returnedTo(studentId, recently, GRADES_LIMIT),
                courseService.announcementsSince(courses.keySet(), recently,
                        ANNOUNCEMENTS_LIMIT));
    }

    private TeacherDashboard teacherPart(Map<UUID, String> courses) {
        return new TeacherDashboard(assignmentService.backlogIn(courses.keySet()),
                courseService.pendingEnrollmentsIn(courses.keySet()));
    }
}
