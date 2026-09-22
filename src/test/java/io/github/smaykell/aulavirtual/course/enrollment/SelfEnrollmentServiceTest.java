package io.github.smaykell.aulavirtual.course.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.Course;
import io.github.smaykell.aulavirtual.course.CourseFixtures;
import io.github.smaykell.aulavirtual.course.CourseRepository;
import io.github.smaykell.aulavirtual.course.enrollment.dto.CourseInvitationResponse;
import io.github.smaykell.aulavirtual.course.enrollment.dto.SelfRegistrationRequest;
import io.github.smaykell.aulavirtual.course.enrollment.dto.SelfRegistrationResponse;
import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.Sex;
import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.settings.SettingsService;
import io.github.smaykell.aulavirtual.settings.StudentIdentifier;
import io.github.smaykell.aulavirtual.settings.dto.SettingsResponse;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.RegisteredStudent;
import io.github.smaykell.aulavirtual.student.exception.StudentAlreadyRegisteredException;
import io.github.smaykell.aulavirtual.teacher.TeacherService;
import io.github.smaykell.aulavirtual.teacher.dto.TeacherSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SelfEnrollmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-10T09:00:00Z");
    private static final String CODE = "ABCD2345";
    private static final UUID STUDENT = UUID.randomUUID();

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private PersonService personService;

    @Mock
    private TeacherService teacherService;

    @Mock
    private SettingsService settingsService;

    @Mock
    private NotificationService notificationService;

    private SelfEnrollmentService selfEnrollmentService;

    @BeforeEach
    void setUp() {
        selfEnrollmentService = new SelfEnrollmentService(courseRepository, enrollmentRepository,
                studentService, personService, teacherService, settingsService,
                notificationService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void the_preview_says_the_course_and_the_teacher_and_nothing_else() {
        Course course = givenTheCourse(EnrollmentPolicy.AUTOMATIC);
        givenTheSelfRegistrationIs(true);
        when(teacherService.summaryOf(course.getTeacherId()))
                .thenReturn(new TeacherSummary(course.getTeacherId(), "Juan", "Perez", true));

        CourseInvitationResponse preview = selfEnrollmentService.preview(CODE);

        assertThat(preview.teacherName()).isEqualTo("Juan Perez");
        assertThat(preview.studentIdentifier()).isEqualTo(StudentIdentifier.DOCUMENT_NUMBER);
        assertThat(preview.open()).isTrue();
    }

    @Test
    void a_course_that_takes_everybody_leaves_the_newcomer_enrolled_and_welcomed() {
        givenTheCourse(EnrollmentPolicy.AUTOMATIC);
        givenTheSelfRegistrationIs(true);
        givenNobodyIsRegistered();
        givenTheStudentIsRegisteredAs("45678912");
        givenTheEnrollmentIsStored();

        SelfRegistrationResponse response = selfEnrollmentService.register(CODE, aRequest());

        assertThat(response.username()).isEqualTo("45678912");
        assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        verify(notificationService).enqueue(eq(NotificationType.ACCOUNT_CREATED),
                eq("ana@escuela.pe"), any());
        verify(notificationService).enqueue(eq(NotificationType.ENROLLMENT_ACTIVE),
                eq("ana@escuela.pe"), any());
    }

    @Test
    void a_course_that_reviews_its_enrollments_leaves_the_newcomer_waiting() {
        givenTheCourse(EnrollmentPolicy.ON_REQUEST);
        givenTheSelfRegistrationIs(true);
        givenNobodyIsRegistered();
        givenTheStudentIsRegisteredAs("45678912");
        givenTheEnrollmentIsStored();

        SelfRegistrationResponse response = selfEnrollmentService.register(CODE, aRequest());

        assertThat(response.status()).isEqualTo(EnrollmentStatus.PENDING);
        verify(notificationService).enqueue(eq(NotificationType.ENROLLMENT_REQUESTED),
                eq("ana@escuela.pe"), any());
    }

    @Test
    void with_the_self_registration_closed_nobody_gets_in_by_the_link() {
        givenTheSelfRegistrationIs(false);

        ApiException error = assertThrows(ApiException.class,
                () -> selfEnrollmentService.register(CODE, aRequest()));

        assertThat(error.getCode()).isEqualTo("CRS_SELF_REGISTRATION_CLOSED");
        verify(studentService, never()).register(any(), any());
    }

    @Test
    void a_code_that_belongs_to_no_course_is_rejected() {
        givenTheSelfRegistrationIs(true);
        when(courseRepository.findByInvitationCode(CODE)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> selfEnrollmentService.register(CODE, aRequest()));

        assertThat(error.getCode()).isEqualTo("CRS_INVALID_INVITATION");
    }

    @Test
    void an_archived_course_says_it_is_closed_and_not_that_it_should_be_activated() {
        givenTheCourse(EnrollmentPolicy.AUTOMATIC).archive();
        givenTheSelfRegistrationIs(true);

        ApiException error = assertThrows(ApiException.class,
                () -> selfEnrollmentService.register(CODE, aRequest()));

        assertThat(error.getCode()).isEqualTo("CRS_COURSE_NOT_OPEN");
    }

    @Test
    void a_document_that_is_already_registered_is_sent_to_the_login() {
        givenTheCourse(EnrollmentPolicy.AUTOMATIC);
        givenTheSelfRegistrationIs(true);
        when(personService.findByDocument(DocumentType.DNI, "45678912"))
                .thenReturn(Optional.of(aPerson()));

        ApiException error = assertThrows(ApiException.class,
                () -> selfEnrollmentService.register(CODE, aRequest()));

        assertThat(error.getCode()).isEqualTo("CRS_ACCOUNT_ALREADY_REGISTERED");
        verify(studentService, never()).register(any(), any());
    }

    @Test
    void an_email_that_is_already_registered_answers_exactly_the_same_as_a_document() {
        givenTheCourse(EnrollmentPolicy.AUTOMATIC);
        givenTheSelfRegistrationIs(true);
        when(personService.findByDocument(DocumentType.DNI, "45678912"))
                .thenReturn(Optional.empty());
        when(personService.findByEmail("ana@escuela.pe")).thenReturn(Optional.of(aPerson()));

        ApiException error = assertThrows(ApiException.class,
                () -> selfEnrollmentService.register(CODE, aRequest()));

        assertThat(error.getCode()).isEqualTo("CRS_ACCOUNT_ALREADY_REGISTERED");
    }

    @Test
    void a_collision_found_further_in_does_not_leak_which_field_collided_either() {
        givenTheCourse(EnrollmentPolicy.AUTOMATIC);
        givenTheSelfRegistrationIs(true);
        givenNobodyIsRegistered();
        when(studentService.register(any(), any()))
                .thenThrow(new StudentAlreadyRegisteredException());

        ApiException error = assertThrows(ApiException.class,
                () -> selfEnrollmentService.register(CODE, aRequest()));

        assertThat(error.getCode()).isEqualTo("CRS_ACCOUNT_ALREADY_REGISTERED");
    }

    @Test
    void a_passport_cannot_be_a_username_so_it_does_not_come_in_by_the_link() {
        givenTheCourse(EnrollmentPolicy.AUTOMATIC);
        givenTheSelfRegistrationIs(true);

        ApiException error = assertThrows(ApiException.class, () -> selfEnrollmentService
                .register(CODE, requestWith(DocumentType.PASSPORT, "AB1234", "ana@escuela.pe")));

        assertThat(error.getCode()).isEqualTo("CRS_SELF_REGISTRATION_DOCUMENT");
    }

    @Test
    void without_an_email_there_is_nowhere_to_send_the_welcome() {
        givenTheCourse(EnrollmentPolicy.AUTOMATIC);
        givenTheSelfRegistrationIs(true);

        ApiException error = assertThrows(ApiException.class, () -> selfEnrollmentService
                .register(CODE, requestWith(DocumentType.DNI, "45678912", null)));

        assertThat(error.getCode()).isEqualTo("PRS_EMAIL_REQUIRED");
    }

    private Course givenTheCourse(EnrollmentPolicy policy) {
        Course course = CourseFixtures.course(UUID.randomUUID(), policy);
        lenient().when(courseRepository.findByInvitationCode(CODE))
                .thenReturn(Optional.of(course));
        return course;
    }

    private void givenTheSelfRegistrationIs(boolean open) {
        when(settingsService.current())
                .thenReturn(new SettingsResponse(StudentIdentifier.DOCUMENT_NUMBER, open));
    }

    private void givenNobodyIsRegistered() {
        when(personService.findByDocument(DocumentType.DNI, "45678912"))
                .thenReturn(Optional.empty());
        when(personService.findByEmail("ana@escuela.pe")).thenReturn(Optional.empty());
    }

    private void givenTheStudentIsRegisteredAs(String username) {
        when(studentService.register(any(), eq("Hospital Regional")))
                .thenReturn(new RegisteredStudent(STUDENT, username));
    }

    private void givenTheEnrollmentIsStored() {
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    private static SelfRegistrationRequest aRequest() {
        return requestWith(DocumentType.DNI, "45678912", "ana@escuela.pe");
    }

    private static SelfRegistrationRequest requestWith(DocumentType type, String documentNumber,
            String email) {

        return new SelfRegistrationRequest(new PersonData(type, documentNumber, "Ana Maria",
                "Quispe Rojas", LocalDate.of(1990, 5, 20), Sex.FEMALE, email),
                "Hospital Regional");
    }

    private static PersonResponse aPerson() {
        return new PersonResponse(UUID.randomUUID(), DocumentType.DNI, "45678912", "Ana Maria",
                "Quispe Rojas", LocalDate.of(1990, 5, 20), Sex.FEMALE, "ana@escuela.pe");
    }
}
