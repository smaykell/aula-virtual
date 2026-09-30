package io.github.smaykell.aulavirtual.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.notification.MessageDates;
import io.github.smaykell.aulavirtual.notification.NotificationProperties;
import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentContact;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentNoticesTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUIS = UUID.randomUUID();

    @Mock
    private CourseService courseService;

    @Mock
    private StudentService studentService;

    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Map<String, String>> data;

    private AssignmentNotices notices;
    private final Assignment assignment = AssignmentFixtures.assignment();

    @BeforeEach
    void setUp() {
        MessageDates dates = new MessageDates(new NotificationProperties(Duration.ofSeconds(30),
                50, 3, Duration.ofMinutes(1), "aula@escuela.pe", ZoneId.of("America/Lima")));
        notices = new AssignmentNotices(courseService, studentService, notificationService,
                dates);
    }

    @Test
    void a_new_task_reaches_every_active_student_with_its_due_date() {
        when(courseService.activeStudentsOf(AssignmentFixtures.COURSE))
                .thenReturn(List.of(ANA, LUIS));
        givenTheCourseAndTheContactsOf(List.of(ANA, LUIS));

        notices.published(assignment);

        verify(notificationService).enqueue(eq(NotificationType.ASSIGNMENT_PUBLISHED),
                eq("ana@escuela.pe"), data.capture());
        verify(notificationService).enqueue(eq(NotificationType.ASSIGNMENT_PUBLISHED),
                eq("luis@escuela.pe"), any());
        assertThat(data.getValue())
                .containsEntry("courseName", "Algebra Lineal")
                .containsEntry("assignmentTitle", "Practica 1")
                .containsEntry("dueAt", "20/04/2026 a las 18:59");
    }

    @Test
    void a_course_without_students_sends_nothing() {
        when(courseService.activeStudentsOf(AssignmentFixtures.COURSE)).thenReturn(List.of());

        notices.published(assignment);

        verifyNoInteractions(studentService, notificationService);
    }

    @Test
    void a_grade_handed_back_is_told_only_to_its_students() {
        givenTheCourseAndTheContactsOf(List.of(ANA));

        notices.handedBack(assignment, List.of(ANA));

        verify(notificationService).enqueue(eq(NotificationType.GRADE_RETURNED),
                eq("ana@escuela.pe"), any());
    }

    private void givenTheCourseAndTheContactsOf(List<UUID> students) {
        when(courseService.nameOf(AssignmentFixtures.COURSE)).thenReturn("Algebra Lineal");
        when(studentService.contactsOf(students)).thenReturn(students.stream()
                .map(id -> id.equals(ANA)
                        ? new StudentContact("Ana", "ana@escuela.pe")
                        : new StudentContact("Luis", "luis@escuela.pe"))
                .toList());
    }
}
