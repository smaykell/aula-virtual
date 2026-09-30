package io.github.smaykell.aulavirtual.course.enrollment;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.course.Course;
import io.github.smaykell.aulavirtual.course.CourseAccess;
import io.github.smaykell.aulavirtual.course.CourseRepository;
import io.github.smaykell.aulavirtual.course.dto.CourseSummary;
import io.github.smaykell.aulavirtual.course.enrollment.dto.DirectEnrollmentOutcome;
import io.github.smaykell.aulavirtual.course.enrollment.dto.DirectEnrollmentRequest;
import io.github.smaykell.aulavirtual.course.enrollment.dto.DirectEnrollmentResult;
import io.github.smaykell.aulavirtual.course.enrollment.dto.EnrollmentResponse;
import io.github.smaykell.aulavirtual.course.enrollment.dto.JoinCourseRequest;
import io.github.smaykell.aulavirtual.course.exception.AlreadyEnrolledException;
import io.github.smaykell.aulavirtual.course.exception.CourseNotOpenException;
import io.github.smaykell.aulavirtual.course.exception.EnrollmentNotActiveException;
import io.github.smaykell.aulavirtual.course.exception.EnrollmentNotFoundException;
import io.github.smaykell.aulavirtual.course.exception.EnrollmentNotPendingException;
import io.github.smaykell.aulavirtual.course.exception.EnrollmentPendingException;
import io.github.smaykell.aulavirtual.course.exception.InvalidInvitationException;
import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentContact;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseAccess courseAccess;
    private final StudentService studentService;
    private final NotificationService notificationService;
    private final Clock clock;

    private record Managed(Enrollment enrollment, Course course) {
    }

    @Transactional
    public EnrollmentResponse join(String actorUsername, JoinCourseRequest request) {
        Course course = courseRepository.findByInvitationCode(request.code().trim())
                .orElseThrow(InvalidInvitationException::new);
        UUID studentId = courseAccess.requireEnrollable(actorUsername, course);
        if (course.isArchived()) {
            throw new CourseNotOpenException();
        }

        Enrollment enrollment = enrollmentRepository
                .findByCourseIdAndStudentId(course.getId(), studentId)
                .map(existing -> askAgain(existing, course))
                .orElseGet(() -> enrollmentRepository.save(Enrollment.request(course.getId(),
                        studentId, course.getEnrollmentPolicy(), clock.instant())));

        announce(enrollment.isPending()
                ? NotificationType.ENROLLMENT_REQUESTED
                : NotificationType.ENROLLMENT_ACTIVE, studentId, course);
        return responseFor(enrollment, course);
    }

    @Transactional
    public List<DirectEnrollmentResult> enroll(String actorUsername, UUID courseId,
            DirectEnrollmentRequest request) {

        Course course = courseAccess.writable(actorUsername, courseId);
        Optional<UUID> titular = courseAccess.titularAsStudent(course);
        return request.documentNumbers().stream()
                .map(String::trim)
                .distinct()
                .map(number -> enrollByDocument(course, titular, request.documentType(), number))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> list(String actorUsername, UUID courseId,
            EnrollmentStatus status, Pageable pageable) {

        Course course = courseAccess.managed(actorUsername, courseId);
        Page<Enrollment> enrollments = status == null
                ? enrollmentRepository.findByCourseId(course.getId(), pageable)
                : enrollmentRepository.findByCourseIdAndStatus(course.getId(), status, pageable);

        CourseSummary summary = CourseSummary.from(course);
        Map<UUID, StudentSummary> students = studentsOf(enrollments.getContent());

        return PageResponse.of(enrollments, enrollment -> EnrollmentResponse.from(enrollment,
                summary, students.get(enrollment.getStudentId())));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> mine(String actorUsername) {
        UUID studentId = courseAccess.requireStudent(actorUsername);
        List<Enrollment> enrollments =
                enrollmentRepository.findByStudentIdOrderByRequestedAtDesc(studentId);

        StudentSummary student = studentService.summaryOf(studentId);
        Map<UUID, CourseSummary> courses = coursesOf(enrollments);

        return enrollments.stream()
                .map(enrollment -> EnrollmentResponse.from(enrollment,
                        courses.get(enrollment.getCourseId()), student))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> mineIn(String actorUsername, String invitationCode) {
        UUID studentId = courseAccess.requireStudent(actorUsername);
        return courseRepository.findByInvitationCode(invitationCode.trim())
                .flatMap(course -> enrollmentRepository
                        .findByCourseIdAndStudentId(course.getId(), studentId)
                        .map(enrollment -> responseFor(enrollment, course)))
                .stream()
                .toList();
    }

    @Transactional
    public EnrollmentResponse accept(String actorUsername, UUID enrollmentId) {
        Managed managed = manageable(actorUsername, enrollmentId);
        Enrollment enrollment = managed.enrollment();
        requirePending(enrollment);
        enrollment.accept(clock.instant());
        announce(NotificationType.ENROLLMENT_ACTIVE, enrollment.getStudentId(), managed.course());
        return responseFor(enrollment, managed.course());
    }

    @Transactional
    public EnrollmentResponse reject(String actorUsername, UUID enrollmentId) {
        Managed managed = manageable(actorUsername, enrollmentId);
        Enrollment enrollment = managed.enrollment();
        requirePending(enrollment);
        enrollment.reject(clock.instant());
        return responseFor(enrollment, managed.course());
    }

    @Transactional
    public EnrollmentResponse withdraw(String actorUsername, UUID enrollmentId) {
        Managed managed = manageable(actorUsername, enrollmentId);
        Enrollment enrollment = managed.enrollment();
        if (!enrollment.isActive()) {
            throw new EnrollmentNotActiveException();
        }
        enrollment.withdraw(clock.instant());
        return responseFor(enrollment, managed.course());
    }

    private DirectEnrollmentResult enrollByDocument(Course course, Optional<UUID> titular,
            DocumentType documentType, String documentNumber) {

        return studentService.findByDocument(documentType, documentNumber)
                .map(student -> enrollStudent(course, titular, student, documentNumber))
                .orElseGet(() -> DirectEnrollmentResult.skipped(documentNumber,
                        DirectEnrollmentOutcome.NOT_A_STUDENT));
    }

    private DirectEnrollmentResult enrollStudent(Course course, Optional<UUID> titular,
            StudentSummary student, String documentNumber) {

        if (!student.active()) {
            return DirectEnrollmentResult.skipped(documentNumber,
                    DirectEnrollmentOutcome.INACTIVE_STUDENT);
        }
        if (titular.filter(student.id()::equals).isPresent()) {
            return DirectEnrollmentResult.skipped(documentNumber, DirectEnrollmentOutcome.TITULAR);
        }
        Optional<Enrollment> existing =
                enrollmentRepository.findByCourseIdAndStudentId(course.getId(), student.id());
        if (existing.filter(Enrollment::isActive).isPresent()) {
            return resultOf(documentNumber, DirectEnrollmentOutcome.ALREADY_ENROLLED,
                    existing.get(), course, student);
        }
        Enrollment enrollment = activate(existing, course, student.id());
        announce(NotificationType.ENROLLMENT_ACTIVE, student.id(), course);
        return resultOf(documentNumber, DirectEnrollmentOutcome.ENROLLED, enrollment, course,
                student);
    }

    private Enrollment activate(Optional<Enrollment> existing, Course course, UUID studentId) {
        existing.ifPresent(enrollment ->
                enrollment.restart(EnrollmentPolicy.AUTOMATIC, clock.instant()));
        return existing.orElseGet(() -> enrollmentRepository.save(Enrollment.request(
                course.getId(), studentId, EnrollmentPolicy.AUTOMATIC, clock.instant())));
    }

    private static DirectEnrollmentResult resultOf(String documentNumber,
            DirectEnrollmentOutcome outcome, Enrollment enrollment, Course course,
            StudentSummary student) {

        return new DirectEnrollmentResult(documentNumber, outcome,
                EnrollmentResponse.from(enrollment, CourseSummary.from(course), student));
    }

    private void announce(NotificationType type, UUID studentId, Course course) {
        StudentContact student = studentService.contactOf(studentId);
        notificationService.enqueue(type, student.email(),
                Map.of("firstName", student.firstName(), "courseName", course.getName()));
    }

    private Enrollment askAgain(Enrollment enrollment, Course course) {
        if (enrollment.isActive()) {
            throw new AlreadyEnrolledException();
        }
        if (enrollment.isPending()) {
            throw new EnrollmentPendingException();
        }
        enrollment.restart(course.getEnrollmentPolicy(), clock.instant());
        return enrollment;
    }

    private Managed manageable(String actorUsername, UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));
        return new Managed(enrollment,
                courseAccess.writable(actorUsername, enrollment.getCourseId()));
    }

    private EnrollmentResponse responseFor(Enrollment enrollment, Course course) {
        return EnrollmentResponse.from(enrollment, CourseSummary.from(course),
                studentService.summaryOf(enrollment.getStudentId()));
    }

    private Map<UUID, StudentSummary> studentsOf(List<Enrollment> enrollments) {
        return studentService.summariesOf(
                enrollments.stream().map(Enrollment::getStudentId).toList());
    }

    private Map<UUID, CourseSummary> coursesOf(List<Enrollment> enrollments) {
        return courseRepository
                .findAllById(enrollments.stream().map(Enrollment::getCourseId).toList())
                .stream()
                .collect(Collectors.toMap(Course::getId, CourseSummary::from));
    }

    private static void requirePending(Enrollment enrollment) {
        if (!enrollment.isPending()) {
            throw new EnrollmentNotPendingException();
        }
    }
}
