package io.github.smaykell.aulavirtual.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.assignment.AssignmentService;
import io.github.smaykell.aulavirtual.assignment.dto.AssignmentBacklog;
import io.github.smaykell.aulavirtual.assignment.dto.UpcomingAssignment;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseCount;
import io.github.smaykell.aulavirtual.dashboard.dto.DashboardResponse;
import io.github.smaykell.aulavirtual.gradebook.GradebookService;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-20T12:00:00Z");
    private static final Instant TWO_WEEKS_AGO = NOW.minus(Duration.ofDays(14));
    private static final UUID PERSON = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID TEACHER = UUID.randomUUID();
    private static final UUID ATTENDED = UUID.randomUUID();
    private static final UUID TAUGHT = UUID.randomUUID();

    @Mock
    private UserService userService;

    @Mock
    private CourseService courseService;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private GradebookService gradebookService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(userService, courseService, assignmentService,
                gradebookService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void a_student_sees_what_is_pending_in_the_courses_it_attends() {
        givenTheActor(Map.of(Role.STUDENT, STUDENT));
        when(courseService.openCoursesAttendedBy(STUDENT)).thenReturn(Map.of(ATTENDED, "Algebra"));
        UpcomingAssignment task = new UpcomingAssignment(UUID.randomUUID(), ATTENDED,
                "Practica 1", NOW.plus(Duration.ofDays(1)));
        when(assignmentService.pendingFor(STUDENT, Set.of(ATTENDED), NOW, TWO_WEEKS_AGO, 10))
                .thenReturn(List.of(task));
        when(gradebookService.returnedTo(STUDENT, TWO_WEEKS_AGO, 5)).thenReturn(List.of());
        when(courseService.announcementsSince(Set.of(ATTENDED), TWO_WEEKS_AGO, 5))
                .thenReturn(List.of());

        DashboardResponse dashboard = dashboardService.of("ana");

        assertThat(dashboard.courses()).containsEntry(ATTENDED, "Algebra");
        assertThat(dashboard.student().upcoming()).containsExactly(task);
        assertThat(dashboard.teacher()).isNull();
    }

    @Test
    void a_teacher_sees_the_work_to_hand_back_and_the_requests_to_review() {
        givenTheActor(Map.of(Role.TEACHER, TEACHER));
        when(courseService.openCoursesTaughtBy(TEACHER)).thenReturn(Map.of(TAUGHT, "Fisica"));
        AssignmentBacklog backlog = new AssignmentBacklog(UUID.randomUUID(), TAUGHT,
                "Informe", NOW, 3);
        when(assignmentService.backlogIn(Set.of(TAUGHT))).thenReturn(List.of(backlog));
        when(courseService.pendingEnrollmentsIn(Set.of(TAUGHT)))
                .thenReturn(List.of(new CourseCount(TAUGHT, 2)));

        DashboardResponse dashboard = dashboardService.of("ana");

        assertThat(dashboard.teacher().toReturn()).containsExactly(backlog);
        assertThat(dashboard.teacher().pendingEnrollments())
                .containsExactly(new CourseCount(TAUGHT, 2));
        assertThat(dashboard.student()).isNull();
    }

    @Test
    void an_administrator_gets_no_course_sections() {
        givenTheActor(Map.of(Role.ADMIN, UUID.randomUUID()));

        DashboardResponse dashboard = dashboardService.of("ana");

        assertThat(dashboard.student()).isNull();
        assertThat(dashboard.teacher()).isNull();
        assertThat(dashboard.courses()).isEmpty();
        verifyNoInteractions(courseService, assignmentService, gradebookService);
    }

    private void givenTheActor(Map<Role, UUID> profiles) {
        when(userService.actor("ana")).thenReturn(new Actor(PERSON, "ana", profiles));
    }
}
