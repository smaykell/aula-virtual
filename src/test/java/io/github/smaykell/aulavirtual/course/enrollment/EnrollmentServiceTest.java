package io.github.smaykell.aulavirtual.course.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.Course;
import io.github.smaykell.aulavirtual.course.CourseAccess;
import io.github.smaykell.aulavirtual.course.CourseFixtures;
import io.github.smaykell.aulavirtual.course.CourseRepository;
import io.github.smaykell.aulavirtual.course.enrollment.dto.DirectEnrollmentOutcome;
import io.github.smaykell.aulavirtual.course.enrollment.dto.DirectEnrollmentRequest;
import io.github.smaykell.aulavirtual.course.enrollment.dto.DirectEnrollmentResult;
import io.github.smaykell.aulavirtual.course.enrollment.dto.EnrollmentResponse;
import io.github.smaykell.aulavirtual.course.enrollment.dto.JoinCourseRequest;
import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentContact;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-10T09:00:00Z");
    private static final UUID TITULAR = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID TITULAR_AS_STUDENT = UUID.randomUUID();

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseAccess courseAccess;

    @Mock
    private StudentService studentService;

    @Mock
    private NotificationService notificationService;

    private EnrollmentService enrollmentService;

    private Course managedCourse;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(enrollmentRepository, courseRepository,
                courseAccess, studentService, notificationService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void a_course_that_accepts_by_itself_leaves_the_student_enrolled_on_the_spot() {
        Course course = givenTheCourseIsJoinable(EnrollmentPolicy.AUTOMATIC);
        givenNoPreviousEnrollment(course);
        givenTheEnrollmentIsStored();
        givenTheStudentCanBeReached();

        EnrollmentResponse enrollment = enrollmentService.join("ana.estudiante", joinRequest());

        assertThat(enrollment.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollment.requestedAt()).isEqualTo(NOW);
        assertThat(enrollment.decidedAt()).isEqualTo(NOW);
        assertThat(enrollment.course().name()).isEqualTo("Algebra Lineal");
        verify(notificationService).enqueue(eq(NotificationType.ENROLLMENT_ACTIVE),
                eq("ana@escuela.pe"), any());
    }

    @Test
    void a_course_that_reviews_its_enrollments_leaves_the_request_pending() {
        Course course = givenTheCourseIsJoinable(EnrollmentPolicy.ON_REQUEST);
        givenNoPreviousEnrollment(course);
        givenTheEnrollmentIsStored();
        givenTheStudentCanBeReached();

        EnrollmentResponse enrollment = enrollmentService.join("ana.estudiante", joinRequest());

        assertThat(enrollment.status()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(enrollment.decidedAt()).isNull();
        verify(notificationService).enqueue(eq(NotificationType.ENROLLMENT_REQUESTED),
                eq("ana@escuela.pe"), any());
    }

    @Test
    void a_code_that_belongs_to_no_course_is_rejected() {
        when(courseRepository.findByInvitationCode("ABCD2345")).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> enrollmentService.join("ana.estudiante", joinRequest()));

        assertThat(error.getCode()).isEqualTo("CRS_INVALID_INVITATION");
    }

    @Test
    void an_archived_course_does_not_take_new_students() {
        Course course = givenTheCourseIsJoinable(EnrollmentPolicy.AUTOMATIC);
        course.archive();

        ApiException error = assertThrows(ApiException.class,
                () -> enrollmentService.join("ana.estudiante", joinRequest()));

        assertThat(error.getCode()).isEqualTo("CRS_COURSE_NOT_OPEN");
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void whoever_is_already_enrolled_does_not_enroll_twice() {
        Course course = givenTheCourseIsJoinable(EnrollmentPolicy.AUTOMATIC);
        givenThePreviousEnrollment(course, EnrollmentStatus.ACTIVE);

        ApiException error = assertThrows(ApiException.class,
                () -> enrollmentService.join("ana.estudiante", joinRequest()));

        assertThat(error.getCode()).isEqualTo("CRS_ALREADY_ENROLLED");
    }

    @Test
    void a_request_that_is_still_pending_is_not_sent_again() {
        Course course = givenTheCourseIsJoinable(EnrollmentPolicy.ON_REQUEST);
        givenThePreviousEnrollment(course, EnrollmentStatus.PENDING);

        ApiException error = assertThrows(ApiException.class,
                () -> enrollmentService.join("ana.estudiante", joinRequest()));

        assertThat(error.getCode()).isEqualTo("CRS_ENROLLMENT_PENDING");
    }

    @Test
    void whoever_was_rejected_can_ask_again_on_the_same_row() {
        Course course = givenTheCourseIsJoinable(EnrollmentPolicy.ON_REQUEST);
        Enrollment enrollment = givenThePreviousEnrollment(course, EnrollmentStatus.REJECTED);
        givenTheSummaryOfTheStudent();
        givenTheStudentCanBeReached();

        EnrollmentResponse response = enrollmentService.join("ana.estudiante", joinRequest());

        assertThat(response.id()).isEqualTo(enrollment.getId());
        assertThat(response.status()).isEqualTo(EnrollmentStatus.PENDING);
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void accepting_a_request_turns_it_into_an_active_enrollment() {
        Enrollment enrollment = givenTheManagedEnrollmentWithItsStudent(EnrollmentStatus.PENDING);
        givenTheStudentCanBeReached();

        EnrollmentResponse accepted = enrollmentService.accept("juan", enrollment.getId());

        assertThat(accepted.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(accepted.decidedAt()).isEqualTo(NOW);
        verify(notificationService).enqueue(eq(NotificationType.ENROLLMENT_ACTIVE),
                eq("ana@escuela.pe"), any());
    }

    @Test
    void rejecting_a_request_closes_it_without_enrolling_anybody() {
        Enrollment enrollment = givenTheManagedEnrollmentWithItsStudent(EnrollmentStatus.PENDING);

        assertThat(enrollmentService.reject("juan", enrollment.getId()).status())
                .isEqualTo(EnrollmentStatus.REJECTED);
    }

    @Test
    void a_request_already_resolved_is_not_resolved_twice() {
        Enrollment enrollment = givenTheManagedEnrollment(EnrollmentStatus.ACTIVE);

        ApiException error = assertThrows(ApiException.class,
                () -> enrollmentService.accept("juan", enrollment.getId()));

        assertThat(error.getCode()).isEqualTo("CRS_ENROLLMENT_NOT_PENDING");
    }

    @Test
    void only_an_active_enrollment_can_be_withdrawn() {
        Enrollment enrollment = givenTheManagedEnrollment(EnrollmentStatus.PENDING);

        ApiException error = assertThrows(ApiException.class,
                () -> enrollmentService.withdraw("juan", enrollment.getId()));

        assertThat(error.getCode()).isEqualTo("CRS_ENROLLMENT_NOT_ACTIVE");
    }

    @Test
    void withdrawing_takes_the_student_out_of_the_course() {
        Enrollment enrollment = givenTheManagedEnrollmentWithItsStudent(EnrollmentStatus.ACTIVE);

        assertThat(enrollmentService.withdraw("juan", enrollment.getId()).status())
                .isEqualTo(EnrollmentStatus.WITHDRAWN);
    }

    @Test
    void resolving_a_request_reuses_the_course_that_the_check_already_read() {
        Enrollment enrollment =
                givenTheManagedEnrollmentWithItsStudent(EnrollmentStatus.PENDING);
        givenTheStudentCanBeReached();

        enrollmentService.accept("juan", enrollment.getId());

        verify(courseRepository, never()).findById(any());
    }

    @Test
    void an_unknown_enrollment_is_not_found() {
        UUID enrollmentId = UUID.randomUUID();
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> enrollmentService.accept("juan", enrollmentId));

        assertThat(error.getCode()).isEqualTo("CRS_ENROLLMENT_NOT_FOUND");
    }

    @Test
    void a_student_finds_its_own_enrollment_through_the_invitation_code() {
        Course course = CourseFixtures.course(TITULAR, EnrollmentPolicy.ON_REQUEST);
        when(courseRepository.findByInvitationCode(CourseFixtures.INVITATION_CODE))
                .thenReturn(Optional.of(course));
        when(courseAccess.requireStudent("ana.estudiante")).thenReturn(STUDENT);
        givenThePreviousEnrollment(course, EnrollmentStatus.PENDING);
        givenTheSummaryOfTheStudent();

        List<EnrollmentResponse> mine =
                enrollmentService.mineIn("ana.estudiante", CourseFixtures.INVITATION_CODE);

        assertThat(mine).singleElement().satisfies(enrollment -> {
            assertThat(enrollment.status()).isEqualTo(EnrollmentStatus.PENDING);
            assertThat(enrollment.course().id()).isEqualTo(course.getId());
        });
    }

    @Test
    void a_code_that_leads_nowhere_finds_no_enrollment() {
        when(courseAccess.requireStudent("ana.estudiante")).thenReturn(STUDENT);
        when(courseRepository.findByInvitationCode("ZZZZ9999")).thenReturn(Optional.empty());

        assertThat(enrollmentService.mineIn("ana.estudiante", "ZZZZ9999")).isEmpty();
    }

    @Test
    void the_staff_enrolls_by_document_even_when_the_course_reviews_requests() {
        Course course = givenTheWritableCourse();
        givenTheStudentWithDocument("45678912", CourseFixtures.student(STUDENT));
        when(enrollmentRepository.findByCourseIdAndStudentId(course.getId(), STUDENT))
                .thenReturn(Optional.empty());
        givenTheEnrollmentIsStored();
        givenTheStudentCanBeReached();

        List<DirectEnrollmentResult> results = enrollmentService.enroll("juan", course.getId(),
                directRequest(" 45678912 ", "45678912"));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.documentNumber()).isEqualTo("45678912");
            assertThat(result.outcome()).isEqualTo(DirectEnrollmentOutcome.ENROLLED);
            assertThat(result.enrollment().status()).isEqualTo(EnrollmentStatus.ACTIVE);
        });
        verify(notificationService).enqueue(eq(NotificationType.ENROLLMENT_ACTIVE),
                eq("ana@escuela.pe"), any());
    }

    @Test
    void a_pending_or_withdrawn_student_is_enrolled_on_the_same_row() {
        Course course = givenTheWritableCourse();
        givenTheStudentWithDocument("45678912", CourseFixtures.student(STUDENT));
        Enrollment previous = givenThePreviousEnrollment(course, EnrollmentStatus.WITHDRAWN);
        givenTheStudentCanBeReached();

        enrollmentService.enroll("juan", course.getId(), directRequest("45678912"));

        assertThat(previous.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void whoever_is_already_enrolled_is_reported_and_not_notified_again() {
        Course course = givenTheWritableCourse();
        givenTheStudentWithDocument("45678912", CourseFixtures.student(STUDENT));
        givenThePreviousEnrollment(course, EnrollmentStatus.ACTIVE);

        List<DirectEnrollmentResult> results = enrollmentService.enroll("juan", course.getId(),
                directRequest("45678912"));

        assertThat(results.get(0).outcome()).isEqualTo(DirectEnrollmentOutcome.ALREADY_ENROLLED);
        verify(notificationService, never()).enqueue(any(), any(), any());
    }

    @Test
    void each_document_that_cannot_be_enrolled_says_why_without_stopping_the_rest() {
        Course course = givenTheWritableCourse();
        when(courseAccess.titularAsStudent(course)).thenReturn(Optional.of(TITULAR_AS_STUDENT));
        givenTheStudentWithDocument("11111111", null);
        givenTheStudentWithDocument("22222222",
                new StudentSummary(UUID.randomUUID(), "Luis", "Rojas", null, false));
        givenTheStudentWithDocument("33333333",
                new StudentSummary(TITULAR_AS_STUDENT, "Juan", "Perez", null, true));

        List<DirectEnrollmentResult> results = enrollmentService.enroll("juan", course.getId(),
                directRequest("11111111", "22222222", "33333333"));

        assertThat(results).extracting(DirectEnrollmentResult::outcome).containsExactly(
                DirectEnrollmentOutcome.NOT_A_STUDENT,
                DirectEnrollmentOutcome.INACTIVE_STUDENT,
                DirectEnrollmentOutcome.TITULAR);
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    private Course givenTheWritableCourse() {
        Course course = CourseFixtures.course(TITULAR, EnrollmentPolicy.ON_REQUEST);
        when(courseAccess.writable("juan", course.getId())).thenReturn(course);
        return course;
    }

    private void givenTheStudentWithDocument(String documentNumber, StudentSummary student) {
        when(studentService.findByDocument(DocumentType.DNI, documentNumber))
                .thenReturn(Optional.ofNullable(student));
    }

    private static DirectEnrollmentRequest directRequest(String... documentNumbers) {
        return new DirectEnrollmentRequest(DocumentType.DNI, List.of(documentNumbers));
    }

    private Course givenTheCourseIsJoinable(EnrollmentPolicy policy) {
        Course course = CourseFixtures.course(TITULAR, policy);
        when(courseRepository.findByInvitationCode(CourseFixtures.INVITATION_CODE))
                .thenReturn(Optional.of(course));
        when(courseAccess.requireEnrollable("ana.estudiante", course)).thenReturn(STUDENT);
        return course;
    }

    private void givenNoPreviousEnrollment(Course course) {
        when(enrollmentRepository.findByCourseIdAndStudentId(course.getId(), STUDENT))
                .thenReturn(Optional.empty());
        givenTheSummaryOfTheStudent();
    }

    private Enrollment givenThePreviousEnrollment(Course course, EnrollmentStatus status) {
        Enrollment enrollment = CourseFixtures.enrollment(course.getId(), STUDENT,
                EnrollmentPolicy.ON_REQUEST, NOW);
        decide(enrollment, status);
        when(enrollmentRepository.findByCourseIdAndStudentId(course.getId(), STUDENT))
                .thenReturn(Optional.of(enrollment));
        return enrollment;
    }

    private Enrollment givenTheManagedEnrollment(EnrollmentStatus status) {
        managedCourse = CourseFixtures.course(TITULAR, EnrollmentPolicy.ON_REQUEST);
        Enrollment enrollment = CourseFixtures.enrollment(managedCourse.getId(), STUDENT,
                EnrollmentPolicy.ON_REQUEST, NOW);
        decide(enrollment, status);

        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));
        when(courseAccess.writable("juan", managedCourse.getId())).thenReturn(managedCourse);
        return enrollment;
    }

    private Enrollment givenTheManagedEnrollmentWithItsStudent(EnrollmentStatus status) {
        Enrollment enrollment = givenTheManagedEnrollment(status);
        givenTheSummaryOfTheStudent();
        return enrollment;
    }

    private void givenTheStudentCanBeReached() {
        when(studentService.contactOf(STUDENT))
                .thenReturn(new StudentContact("Ana Maria", "ana@escuela.pe"));
    }

    private void givenTheSummaryOfTheStudent() {
        when(studentService.summaryOf(STUDENT)).thenReturn(CourseFixtures.student(STUDENT));
    }

    private void givenTheEnrollmentIsStored() {
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(call -> {
            Enrollment enrollment = call.getArgument(0);
            ReflectionTestUtils.setField(enrollment, "id", UUID.randomUUID());
            return enrollment;
        });
    }

    private static void decide(Enrollment enrollment, EnrollmentStatus status) {
        ReflectionTestUtils.setField(enrollment, "status", status);
    }

    private static JoinCourseRequest joinRequest() {
        return new JoinCourseRequest(CourseFixtures.INVITATION_CODE);
    }
}
