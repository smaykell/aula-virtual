package io.github.smaykell.aulavirtual.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.course.CourseService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentRemindersTest {

    private static final Instant NOW = Instant.parse("2026-04-20T00:00:00Z");
    private static final Duration LEAD = Duration.ofHours(24);
    private static final UUID HANDED_IN = UUID.randomUUID();
    private static final UUID MISSING = UUID.randomUUID();

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private AssignmentNotices notices;

    private AssignmentReminders reminders;

    @BeforeEach
    void setUp() {
        reminders = new AssignmentReminders(assignmentRepository, submissionRepository,
                courseService, notices, new AssignmentProperties(LEAD, Duration.ofMinutes(15)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void only_the_students_who_have_not_handed_in_are_reminded_and_only_once() {
        Assignment assignment = AssignmentFixtures.assignment(NOW.plus(Duration.ofHours(10)),
                false);
        when(assignmentRepository.findByRemindedAtIsNullAndDueAtBetween(NOW, NOW.plus(LEAD)))
                .thenReturn(List.of(assignment));
        when(submissionRepository.findStudentIdsByAssignmentId(assignment.getId()))
                .thenReturn(Set.of(HANDED_IN));
        when(courseService.activeStudentsOf(AssignmentFixtures.COURSE))
                .thenReturn(List.of(HANDED_IN, MISSING));

        reminders.remindDueSoon();

        verify(notices).dueSoon(assignment, List.of(MISSING));
        assertThat(assignment.getRemindedAt()).isEqualTo(NOW);
    }

    @Test
    void moving_the_due_date_allows_a_new_reminder() {
        Assignment assignment = AssignmentFixtures.assignment();
        assignment.markReminded(NOW);

        assignment.update(AssignmentFixtures.data(NOW.plus(Duration.ofDays(7)), false));

        assertThat(assignment.getRemindedAt()).isNull();
    }

    @Test
    void editing_anything_else_keeps_the_reminder_already_sent() {
        Assignment assignment = AssignmentFixtures.assignment();
        assignment.markReminded(NOW);

        assignment.update(AssignmentFixtures.data());

        assertThat(assignment.getRemindedAt()).isEqualTo(NOW);
    }
}
