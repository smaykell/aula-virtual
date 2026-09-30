package io.github.smaykell.aulavirtual.course.announcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.Course;
import io.github.smaykell.aulavirtual.course.CourseAccess;
import io.github.smaykell.aulavirtual.course.CourseFixtures;
import io.github.smaykell.aulavirtual.course.announcement.dto.AnnouncementData;
import io.github.smaykell.aulavirtual.course.announcement.dto.AnnouncementResponse;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentRepository;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentStatus;
import io.github.smaykell.aulavirtual.course.exception.ArchivedCourseException;
import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.Sex;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentContact;
import io.github.smaykell.aulavirtual.user.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    private static final UUID TITULAR = UUID.randomUUID();
    private static final UUID AUTHOR = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final AnnouncementData DATA =
            new AnnouncementData("  Examen parcial ", " Será el lunes a las 8. ");

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseAccess courseAccess;

    @Mock
    private UserService userService;

    @Mock
    private PersonService personService;

    @Mock
    private StudentService studentService;

    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Map<String, String>> data;

    private AnnouncementService announcementService;
    private Course course;

    @BeforeEach
    void setUp() {
        announcementService = new AnnouncementService(announcementRepository,
                enrollmentRepository, courseAccess, userService, personService, studentService,
                notificationService);
        course = CourseFixtures.course(TITULAR);
    }

    @Test
    void publishing_signs_the_announcement_and_tells_every_active_student() {
        givenTheWritableCourse();
        givenTheAuthor();
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(call -> withId(call.getArgument(0)));
        when(enrollmentRepository.findStudentIdsByCourseIdAndStatus(course.getId(),
                EnrollmentStatus.ACTIVE)).thenReturn(List.of(STUDENT));
        when(studentService.contactsOf(List.of(STUDENT)))
                .thenReturn(List.of(new StudentContact("Ana", "ana@escuela.pe")));

        AnnouncementResponse published = announcementService.publish("juan", course.getId(),
                DATA);

        assertThat(published.title()).isEqualTo("Examen parcial");
        assertThat(published.body()).isEqualTo("Será el lunes a las 8.");
        assertThat(published.authorName()).isEqualTo("Juan Perez");
        verify(notificationService).enqueue(eq(NotificationType.ANNOUNCEMENT_PUBLISHED),
                eq("ana@escuela.pe"), data.capture());
        assertThat(data.getValue()).containsEntry("body", "Será el lunes a las 8.");
    }

    @Test
    void a_course_without_students_publishes_without_sending_anything() {
        givenTheWritableCourse();
        givenTheAuthor();
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(call -> withId(call.getArgument(0)));
        when(enrollmentRepository.findStudentIdsByCourseIdAndStatus(course.getId(),
                EnrollmentStatus.ACTIVE)).thenReturn(List.of());

        announcementService.publish("juan", course.getId(), DATA);

        verifyNoInteractions(studentService, notificationService);
    }

    @Test
    void an_archived_course_takes_no_announcements() {
        when(courseAccess.writable("juan", course.getId()))
                .thenThrow(new ArchivedCourseException());

        ApiException error = assertThrows(ApiException.class,
                () -> announcementService.publish("juan", course.getId(), DATA));

        assertThat(error.getCode()).isEqualTo("CRS_ARCHIVED");
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void editing_checks_the_course_of_the_announcement() {
        Announcement announcement = withId(Announcement.publish(course.getId(), AUTHOR, DATA));
        when(announcementRepository.findById(announcement.getId()))
                .thenReturn(Optional.of(announcement));
        when(personService.get(AUTHOR)).thenReturn(author());

        announcementService.update("juan", announcement.getId(),
                new AnnouncementData("Examen parcial", "Se pasa al martes."));

        verify(courseAccess).writable("juan", course.getId());
        assertThat(announcement.getBody()).isEqualTo("Se pasa al martes.");
    }

    @Test
    void an_unknown_announcement_is_not_found() {
        UUID id = UUID.randomUUID();
        when(announcementRepository.findById(id)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> announcementService.delete("juan", id));

        assertThat(error.getCode()).isEqualTo("CRS_ANNOUNCEMENT_NOT_FOUND");
    }

    private void givenTheWritableCourse() {
        when(courseAccess.writable("juan", course.getId())).thenReturn(course);
    }

    private void givenTheAuthor() {
        when(userService.actor("juan"))
                .thenReturn(new Actor(AUTHOR, "juan", Map.of(Role.TEACHER, TITULAR)));
        when(personService.get(AUTHOR)).thenReturn(author());
    }

    private static PersonResponse author() {
        return new PersonResponse(AUTHOR, DocumentType.DNI, "40000000", "Juan", "Perez",
                LocalDate.of(1980, 1, 1), Sex.MALE, "juan@escuela.pe");
    }

    private static Announcement withId(Announcement announcement) {
        ReflectionTestUtils.setField(announcement, "id", UUID.randomUUID());
        return announcement;
    }
}
