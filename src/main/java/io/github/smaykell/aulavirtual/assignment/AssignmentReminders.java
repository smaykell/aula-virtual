package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.course.CourseService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AssignmentReminders {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseService courseService;
    private final AssignmentNotices notices;
    private final AssignmentProperties properties;
    private final Clock clock;

    @Transactional
    public void remindDueSoon() {
        Instant now = clock.instant();
        List<Assignment> dueSoon = assignmentRepository
                .findByRemindedAtIsNullAndDueAtBetween(now, now.plus(properties.reminderLead()));
        for (Assignment assignment : dueSoon) {
            notices.dueSoon(assignment, studentsWithoutWork(assignment));
            assignment.markReminded(now);
        }
    }

    private List<UUID> studentsWithoutWork(Assignment assignment) {
        Set<UUID> handedIn = submissionRepository.findStudentIdsByAssignmentId(assignment.getId());
        return courseService.activeStudentsOf(assignment.getCourseId()).stream()
                .filter(studentId -> !handedIn.contains(studentId))
                .toList();
    }
}
