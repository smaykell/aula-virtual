package io.github.smaykell.aulavirtual.course;

import io.github.smaykell.aulavirtual.common.domain.Filters;
import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.course.announcement.AnnouncementRepository;
import io.github.smaykell.aulavirtual.course.announcement.dto.AnnouncementSummary;
import io.github.smaykell.aulavirtual.course.dto.CourseCount;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.course.dto.CourseResponse;
import io.github.smaykell.aulavirtual.course.dto.CreateCourseRequest;
import io.github.smaykell.aulavirtual.course.dto.UpdateCourseRequest;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentRepository;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentStatus;
import io.github.smaykell.aulavirtual.course.exception.CourseNotFoundException;
import io.github.smaykell.aulavirtual.course.exception.InvalidCourseDatesException;
import io.github.smaykell.aulavirtual.course.exception.StudentNotEnrolledException;
import io.github.smaykell.aulavirtual.teacher.TeacherService;
import io.github.smaykell.aulavirtual.teacher.dto.TeacherSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseAccess courseAccess;
    private final Invitations invitations;
    private final TeacherService teacherService;
    private final EnrollmentRepository enrollmentRepository;
    private final AnnouncementRepository announcementRepository;

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> list(String actorUsername, UUID teacherId,
            CourseStatus status, String search, Pageable pageable) {

        CourseAccess.Scope scope = courseAccess.listingScope(actorUsername, teacherId);
        Page<Course> courses = coursesIn(scope, status, Filters.containing(search), pageable);

        List<UUID> teacherIds = courses.getContent().stream().map(Course::getTeacherId).toList();
        Map<UUID, TeacherSummary> teachers = teacherService.summariesOf(teacherIds);

        if (!scope.staff()) {
            return PageResponse.of(courses, course -> CourseResponse.forStudent(course,
                    teachers.get(course.getTeacherId())));
        }
        Set<UUID> attended = courseAccess.attendedAmong(actorUsername,
                courses.getContent().stream().map(Course::getId).toList());
        return PageResponse.of(courses, course -> attended.contains(course.getId())
                ? CourseResponse.forStudent(course, teachers.get(course.getTeacherId()))
                : CourseResponse.forStaff(course, teachers.get(course.getTeacherId()),
                        invitations.of(course.getInvitationCode())));
    }

    @Transactional(readOnly = true)
    public CourseResponse get(String actorUsername, UUID courseId) {
        CourseAccess.Reader reader = courseAccess.readable(actorUsername, courseId);
        return reader.staff() ? responseFor(reader.course())
                : CourseResponse.forStudent(reader.course(), teacherOf(reader.course()));
    }

    @Transactional
    public CourseResponse create(String actorUsername, CreateCourseRequest request) {
        UUID teacherId = courseAccess.resolveTitular(actorUsername, request.teacherId());
        requireOrderedDates(request.startDate(), request.endDate());

        return responseFor(courseRepository.save(
                Course.create(request, teacherId, invitations.nextCode())));
    }

    @Transactional
    public CourseResponse update(String actorUsername, UUID courseId,
            UpdateCourseRequest request) {

        Course course = courseAccess.writable(actorUsername, courseId);
        courseAccess.requireTitular(actorUsername, course, request.teacherId());
        requireOrderedDates(request.startDate(), request.endDate());

        course.update(request);
        return responseFor(course);
    }

    @Transactional
    public CourseResponse archive(String actorUsername, UUID courseId) {
        Course course = courseAccess.managed(actorUsername, courseId);
        course.archive();
        return responseFor(course);
    }

    @Transactional
    public CourseResponse activate(String actorUsername, UUID courseId) {
        Course course = courseAccess.managed(actorUsername, courseId);
        course.activate();
        return responseFor(course);
    }

    @Transactional(readOnly = true)
    public CourseMember memberOf(String actorUsername, UUID courseId) {
        CourseAccess.Reader reader = courseAccess.readable(actorUsername, courseId);
        return new CourseMember(courseId, reader.staff(), reader.studentId());
    }

    @Transactional(readOnly = true)
    public void requireWritable(String actorUsername, UUID courseId) {
        courseAccess.writable(actorUsername, courseId);
    }

    @Transactional(readOnly = true)
    public void requireActiveStudent(UUID courseId, UUID studentId) {
        if (!enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(courseId, studentId,
                EnrollmentStatus.ACTIVE)) {
            throw new StudentNotEnrolledException();
        }
    }

    @Transactional(readOnly = true)
    public String nameOf(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId))
                .getName();
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> openCoursesAttendedBy(UUID studentId) {
        return namesOf(courseRepository.findAttendedBy(studentId, EnrollmentStatus.ACTIVE,
                CourseStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> openCoursesTaughtBy(UUID teacherId) {
        return namesOf(courseRepository.findByTeacherIdAndStatus(teacherId, CourseStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public List<CourseCount> pendingEnrollmentsIn(Collection<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return enrollmentRepository.countByCourse(courseIds, EnrollmentStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementSummary> announcementsSince(Collection<UUID> courseIds,
            Instant since, int limit) {

        if (courseIds.isEmpty()) {
            return List.of();
        }
        return announcementRepository
                .findByCourseIdInAndCreatedAtAfterOrderByCreatedAtDesc(courseIds, since,
                        Limit.of(limit))
                .stream()
                .map(AnnouncementSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> activeStudentsOf(UUID courseId) {
        return enrollmentRepository.findStudentIdsByCourseIdAndStatus(courseId,
                EnrollmentStatus.ACTIVE);
    }

    private static Map<UUID, String> namesOf(List<Course> courses) {
        return courses.stream().collect(Collectors.toMap(Course::getId, Course::getName,
                (first, second) -> first, LinkedHashMap::new));
    }

    private Page<Course> coursesIn(CourseAccess.Scope scope, CourseStatus status,
            String namePattern, Pageable pageable) {

        return scope.staff()
                ? courseRepository.search(scope.teacherId(), status, namePattern, pageable)
                : courseRepository.searchEnrolled(scope.studentId(), EnrollmentStatus.ACTIVE,
                        status, namePattern, pageable);
    }

    private CourseResponse responseFor(Course course) {
        return CourseResponse.forStaff(course, teacherOf(course),
                invitations.of(course.getInvitationCode()));
    }

    private TeacherSummary teacherOf(Course course) {
        return teacherService.summaryOf(course.getTeacherId());
    }

    private static void requireOrderedDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidCourseDatesException();
        }
    }
}
