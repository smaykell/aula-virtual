package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.notification.MessageDates;
import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentContact;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AssignmentNotices {

    private final CourseService courseService;
    private final StudentService studentService;
    private final NotificationService notificationService;
    private final MessageDates messageDates;

    void published(Assignment assignment) {
        notify(NotificationType.ASSIGNMENT_PUBLISHED, assignment,
                courseService.activeStudentsOf(assignment.getCourseId()));
    }

    void handedBack(Assignment assignment, Collection<UUID> studentIds) {
        notify(NotificationType.GRADE_RETURNED, assignment, studentIds);
    }

    private void notify(NotificationType type, Assignment assignment,
            Collection<UUID> studentIds) {

        if (studentIds.isEmpty()) {
            return;
        }
        String courseName = courseService.nameOf(assignment.getCourseId());
        for (StudentContact student : studentService.contactsOf(studentIds)) {
            notificationService.enqueue(type, student.email(), Map.of(
                    "firstName", student.firstName(),
                    "courseName", courseName,
                    "assignmentTitle", assignment.getTitle(),
                    "dueAt", messageDates.of(assignment.getDueAt())));
        }
    }
}
