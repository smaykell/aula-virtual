package io.github.smaykell.aulavirtual.course.enrollment;

import io.github.smaykell.aulavirtual.course.Course;
import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentContact;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import io.github.smaykell.aulavirtual.teacher.TeacherService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class EnrollmentNotices {

    private final StudentService studentService;
    private final TeacherService teacherService;
    private final PersonService personService;
    private final NotificationService notificationService;

    void joined(Course course, Enrollment enrollment) {
        if (enrollment.isPending()) {
            requested(course, enrollment.getStudentId());
        } else {
            admitted(course, enrollment.getStudentId());
        }
    }

    void admitted(Course course, UUID studentId) {
        toStudent(NotificationType.ENROLLMENT_ACTIVE, course, studentId);
    }

    void rejected(Course course, UUID studentId) {
        toStudent(NotificationType.ENROLLMENT_REJECTED, course, studentId);
    }

    private void requested(Course course, UUID studentId) {
        toStudent(NotificationType.ENROLLMENT_REQUESTED, course, studentId);
        toTitular(course, studentService.summaryOf(studentId));
    }

    private void toStudent(NotificationType type, Course course, UUID studentId) {
        StudentContact student = studentService.contactOf(studentId);
        notificationService.enqueue(type, student.email(),
                Map.of("firstName", student.firstName(), "courseName", course.getName()));
    }

    private void toTitular(Course course, StudentSummary student) {
        PersonResponse titular = personService.get(teacherService.personOf(course.getTeacherId()));
        notificationService.enqueue(NotificationType.ENROLLMENT_TO_REVIEW, titular.email(),
                Map.of("firstName", titular.firstName(), "courseName", course.getName(),
                        "studentName", student.firstName() + " " + student.lastName()));
    }
}
