package io.github.smaykell.aulavirtual.modules.course;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.modules.course.dto.CourseResponse;
import io.github.smaykell.aulavirtual.modules.course.dto.CreateCourseRequest;
import io.github.smaykell.aulavirtual.modules.course.dto.UpdateCourseRequest;
import io.github.smaykell.aulavirtual.modules.course.exception.InvalidCourseDatesException;
import io.github.smaykell.aulavirtual.modules.teacher.TeacherService;
import io.github.smaykell.aulavirtual.modules.teacher.dto.TeacherSummary;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> list(String actorUsername, UUID teacherId,
            CourseStatus status, Pageable pageable) {

        CourseAccess.Scope scope = courseAccess.listingScope(actorUsername, teacherId);
        Page<Course> courses = coursesIn(scope, status, pageable);

        List<UUID> teacherIds = courses.getContent().stream().map(Course::getTeacherId).toList();
        Map<UUID, TeacherSummary> teachers = teacherService.summariesOf(teacherIds);

        if (scope.staff()) {
            return PageResponse.of(courses, course -> CourseResponse.from(course,
                    teachers.get(course.getTeacherId()),
                    invitations.of(course.getInvitationCode())));
        }
        return PageResponse.of(courses, course -> CourseResponse.withoutInvitation(course,
                teachers.get(course.getTeacherId())));
    }

    @Transactional(readOnly = true)
    public CourseResponse get(String actorUsername, UUID courseId) {
        CourseAccess.Reader reader = courseAccess.readable(actorUsername, courseId);
        return reader.staff() ? responseFor(reader.course())
                : withoutInvitation(reader.course());
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
        courseAccess.requireTitular(actorUsername, request.teacherId());
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

    private Page<Course> coursesIn(CourseAccess.Scope scope, CourseStatus status,
            Pageable pageable) {

        return scope.staff()
                ? courseRepository.search(scope.teacherId(), status, pageable)
                : courseRepository.searchEnrolled(scope.studentId(), EnrollmentStatus.ACTIVE,
                        status, pageable);
    }

    private CourseResponse responseFor(Course course) {
        return CourseResponse.from(course, teacherOf(course),
                invitations.of(course.getInvitationCode()));
    }

    private CourseResponse withoutInvitation(Course course) {
        return CourseResponse.withoutInvitation(course, teacherOf(course));
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
