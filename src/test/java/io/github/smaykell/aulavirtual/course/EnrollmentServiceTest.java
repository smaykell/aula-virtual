package io.github.smaykell.aulavirtual.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.dto.EnrollmentResponse;
import io.github.smaykell.aulavirtual.course.dto.JoinCourseRequest;
import io.github.smaykell.aulavirtual.student.StudentService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseAccess courseAccess;

    @Mock
    private StudentService studentService;

    private EnrollmentService enrollmentService;

    private Course managedCourse;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(enrollmentRepository, courseRepository,
                courseAccess, studentService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void a_course_that_accepts_by_itself_leaves_the_student_enrolled_on_the_spot() {
        Course course = givenTheCourseIsJoinable(EnrollmentPolicy.AUTOMATIC);
        givenNoPreviousEnrollment(course);
        givenTheEnrollmentIsStored();

        EnrollmentResponse enrollment = enrollmentService.join("ana.estudiante", joinRequest());

        assertThat(enrollment.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollment.requestedAt()).isEqualTo(NOW);
        assertThat(enrollment.decidedAt()).isEqualTo(NOW);
        assertThat(enrollment.course().name()).isEqualTo("Algebra Lineal");
    }

    @Test
    void a_course_that_reviews_its_enrollments_leaves_the_request_pending() {
        Course course = givenTheCourseIsJoinable(EnrollmentPolicy.ON_REQUEST);
        givenNoPreviousEnrollment(course);
        givenTheEnrollmentIsStored();

        EnrollmentResponse enrollment = enrollmentService.join("ana.estudiante", joinRequest());

        assertThat(enrollment.status()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(enrollment.decidedAt()).isNull();
    }

    @Test
    void a_code_that_belongs_to_no_course_is_rejected() {
        when(courseAccess.requireStudent("ana.estudiante")).thenReturn(STUDENT);
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

        assertThat(error.getCode()).isEqualTo("CRS_ARCHIVED");
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

        EnrollmentResponse response = enrollmentService.join("ana.estudiante", joinRequest());

        assertThat(response.id()).isEqualTo(enrollment.getId());
        assertThat(response.status()).isEqualTo(EnrollmentStatus.PENDING);
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void accepting_a_request_turns_it_into_an_active_enrollment() {
        Enrollment enrollment = givenTheManagedEnrollmentReadBack(EnrollmentStatus.PENDING);

        EnrollmentResponse accepted = enrollmentService.accept("juan", enrollment.getId());

        assertThat(accepted.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(accepted.decidedAt()).isEqualTo(NOW);
    }

    @Test
    void rejecting_a_request_closes_it_without_enrolling_anybody() {
        Enrollment enrollment = givenTheManagedEnrollmentReadBack(EnrollmentStatus.PENDING);

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
        Enrollment enrollment = givenTheManagedEnrollmentReadBack(EnrollmentStatus.ACTIVE);

        assertThat(enrollmentService.withdraw("juan", enrollment.getId()).status())
                .isEqualTo(EnrollmentStatus.WITHDRAWN);
    }

    @Test
    void an_unknown_enrollment_is_not_found() {
        UUID enrollmentId = UUID.randomUUID();
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> enrollmentService.accept("juan", enrollmentId));

        assertThat(error.getCode()).isEqualTo("CRS_ENROLLMENT_NOT_FOUND");
    }

    private Course givenTheCourseIsJoinable(EnrollmentPolicy policy) {
        Course course = CourseFixtures.course(TITULAR, policy);
        when(courseAccess.requireStudent("ana.estudiante")).thenReturn(STUDENT);
        when(courseRepository.findByInvitationCode(CourseFixtures.INVITATION_CODE))
                .thenReturn(Optional.of(course));
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

    private Enrollment givenTheManagedEnrollmentReadBack(EnrollmentStatus status) {
        Enrollment enrollment = givenTheManagedEnrollment(status);
        givenItsCourseAndStudentAreReadBack();
        return enrollment;
    }

    private void givenItsCourseAndStudentAreReadBack() {
        when(courseRepository.findById(managedCourse.getId()))
                .thenReturn(Optional.of(managedCourse));
        givenTheSummaryOfTheStudent();
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
