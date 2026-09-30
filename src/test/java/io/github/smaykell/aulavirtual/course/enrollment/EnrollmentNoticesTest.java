package io.github.smaykell.aulavirtual.course.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.course.Course;
import io.github.smaykell.aulavirtual.course.CourseFixtures;
import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.Sex;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentContact;
import io.github.smaykell.aulavirtual.teacher.TeacherService;
import java.time.Instant;
import java.time.LocalDate;
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
class EnrollmentNoticesTest {

    private static final UUID TITULAR = UUID.randomUUID();
    private static final UUID TITULAR_PERSON = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-03-10T09:00:00Z");

    @Mock
    private StudentService studentService;

    @Mock
    private TeacherService teacherService;

    @Mock
    private PersonService personService;

    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Map<String, String>> data;

    private EnrollmentNotices notices;
    private Course course;

    @BeforeEach
    void setUp() {
        notices = new EnrollmentNotices(studentService, teacherService, personService,
                notificationService);
        course = CourseFixtures.course(TITULAR, EnrollmentPolicy.ON_REQUEST);
        when(studentService.contactOf(STUDENT))
                .thenReturn(new StudentContact("Ana Maria", "ana@escuela.pe"));
    }

    @Test
    void a_request_to_review_reaches_the_student_and_the_titular() {
        when(studentService.summaryOf(STUDENT)).thenReturn(CourseFixtures.student(STUDENT));
        when(teacherService.personOf(TITULAR)).thenReturn(TITULAR_PERSON);
        when(personService.get(TITULAR_PERSON)).thenReturn(new PersonResponse(TITULAR_PERSON,
                DocumentType.DNI, "40000000", "Juan", "Perez", LocalDate.of(1980, 1, 1),
                Sex.MALE, "juan@escuela.pe"));

        notices.joined(course, CourseFixtures.enrollment(course.getId(), STUDENT,
                EnrollmentPolicy.ON_REQUEST, NOW));

        verify(notificationService).enqueue(eq(NotificationType.ENROLLMENT_REQUESTED),
                eq("ana@escuela.pe"), any());
        verify(notificationService).enqueue(eq(NotificationType.ENROLLMENT_TO_REVIEW),
                eq("juan@escuela.pe"), data.capture());
        assertThat(data.getValue()).containsEntry("studentName", "Ana Maria Quispe Rojas")
                .containsEntry("firstName", "Juan");
    }

    @Test
    void joining_a_course_that_takes_everybody_only_welcomes_the_student() {
        notices.joined(course, CourseFixtures.enrollment(course.getId(), STUDENT,
                EnrollmentPolicy.AUTOMATIC, NOW));

        verify(notificationService).enqueue(eq(NotificationType.ENROLLMENT_ACTIVE),
                eq("ana@escuela.pe"), any());
        verify(notificationService, never()).enqueue(eq(NotificationType.ENROLLMENT_TO_REVIEW),
                any(), any());
    }

    @Test
    void a_rejection_is_told_to_the_student() {
        notices.rejected(course, STUDENT);

        verify(notificationService).enqueue(eq(NotificationType.ENROLLMENT_REJECTED),
                eq("ana@escuela.pe"), data.capture());
        assertThat(data.getValue()).containsEntry("courseName", course.getName());
    }
}
