package io.github.smaykell.aulavirtual.course;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.course.dto.CourseSummary;
import io.github.smaykell.aulavirtual.course.dto.EnrollmentResponse;
import io.github.smaykell.aulavirtual.course.dto.JoinCourseRequest;
import io.github.smaykell.aulavirtual.course.exception.AlreadyEnrolledException;
import io.github.smaykell.aulavirtual.course.exception.ArchivedCourseException;
import io.github.smaykell.aulavirtual.course.exception.CourseNotFoundException;
import io.github.smaykell.aulavirtual.course.exception.EnrollmentNotActiveException;
import io.github.smaykell.aulavirtual.course.exception.EnrollmentNotFoundException;
import io.github.smaykell.aulavirtual.course.exception.EnrollmentNotPendingException;
import io.github.smaykell.aulavirtual.course.exception.EnrollmentPendingException;
import io.github.smaykell.aulavirtual.course.exception.InvalidInvitationException;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.time.Clock;
import java.util.List;
import java.util.Map;
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
    private final Clock clock;

    @Transactional
    public EnrollmentResponse join(String actorUsername, JoinCourseRequest request) {
        UUID studentId = courseAccess.requireStudent(actorUsername);
        Course course = courseRepository.findByInvitationCode(request.code().trim())
                .orElseThrow(InvalidInvitationException::new);
        if (course.isArchived()) {
            throw new ArchivedCourseException();
        }

        return responseFor(enrollmentRepository
                .findByCourseIdAndStudentId(course.getId(), studentId)
                .map(enrollment -> askAgain(enrollment, course))
                .orElseGet(() -> enrollmentRepository.save(Enrollment.request(course.getId(),
                        studentId, course.getEnrollmentPolicy(), clock.instant()))),
                course);
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

    @Transactional
    public EnrollmentResponse accept(String actorUsername, UUID enrollmentId) {
        Enrollment enrollment = manageable(actorUsername, enrollmentId);
        requirePending(enrollment);
        enrollment.accept(clock.instant());
        return responseFor(enrollment);
    }

    @Transactional
    public EnrollmentResponse reject(String actorUsername, UUID enrollmentId) {
        Enrollment enrollment = manageable(actorUsername, enrollmentId);
        requirePending(enrollment);
        enrollment.reject(clock.instant());
        return responseFor(enrollment);
    }

    @Transactional
    public EnrollmentResponse withdraw(String actorUsername, UUID enrollmentId) {
        Enrollment enrollment = manageable(actorUsername, enrollmentId);
        if (!enrollment.isActive()) {
            throw new EnrollmentNotActiveException();
        }
        enrollment.withdraw(clock.instant());
        return responseFor(enrollment);
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

    private Enrollment manageable(String actorUsername, UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));
        courseAccess.writable(actorUsername, enrollment.getCourseId());
        return enrollment;
    }

    private EnrollmentResponse responseFor(Enrollment enrollment) {
        UUID courseId = enrollment.getCourseId();
        return responseFor(enrollment, courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId)));
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
