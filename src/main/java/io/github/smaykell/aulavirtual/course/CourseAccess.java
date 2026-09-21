package io.github.smaykell.aulavirtual.course;

import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentRepository;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentStatus;
import io.github.smaykell.aulavirtual.course.exception.ArchivedCourseException;
import io.github.smaykell.aulavirtual.course.exception.CourseNotFoundException;
import io.github.smaykell.aulavirtual.course.exception.CourseOutOfReachException;
import io.github.smaykell.aulavirtual.course.exception.StudentRequiredException;
import io.github.smaykell.aulavirtual.course.exception.TeacherRequiredException;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.teacher.TeacherService;
import io.github.smaykell.aulavirtual.user.UserService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseAccess {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserService userService;
    private final TeacherService teacherService;

    public record Reader(Course course, boolean staff, UUID studentId) {
    }

    public record Scope(UUID teacherId, UUID studentId) {

        public boolean staff() {
            return studentId == null;
        }
    }

    public Reader readable(String actorUsername, UUID courseId) {
        Course course = existing(courseId);
        Actor actor = userService.actor(actorUsername);
        if (staffOver(actor, course.getTeacherId())) {
            return new Reader(course, true, null);
        }
        return new Reader(course, false, enrolledStudentIn(actor, course)
                .orElseThrow(CourseOutOfReachException::new));
    }

    public Course managed(String actorUsername, UUID courseId) {
        Course course = existing(courseId);
        requireStaffOver(userService.actor(actorUsername), course.getTeacherId());
        return course;
    }

    public Course writable(String actorUsername, UUID courseId) {
        Course course = managed(actorUsername, courseId);
        if (course.isArchived()) {
            throw new ArchivedCourseException();
        }
        return course;
    }

    public UUID resolveTitular(String actorUsername, UUID requestedTeacherId) {
        Actor actor = userService.actor(actorUsername);
        UUID teacherId = requestedTeacherId == null ? ownTeacherProfile(actor)
                : requestedTeacherId;
        requireStaffOver(actor, teacherId);
        teacherService.requireActive(teacherId);
        return teacherId;
    }

    public void requireTitular(String actorUsername, UUID teacherId) {
        requireStaffOver(userService.actor(actorUsername), teacherId);
        teacherService.requireActive(teacherId);
    }

    public Scope listingScope(String actorUsername, UUID requestedTeacherId) {
        Actor actor = userService.actor(actorUsername);
        if (actor.canManage(Role.TEACHER)) {
            return new Scope(requestedTeacherId, null);
        }
        UUID ownTeacher = actor.profileId(Role.TEACHER).orElse(null);
        if (ownTeacher != null) {
            requireSameTeacher(requestedTeacherId, ownTeacher);
            return new Scope(ownTeacher, null);
        }
        return new Scope(null, ownStudentProfile(actor));
    }

    public UUID requireStudent(String actorUsername) {
        return ownStudentProfile(userService.actor(actorUsername));
    }

    private static boolean staffOver(Actor actor, UUID teacherId) {
        return actor.canManage(Role.TEACHER)
                || actor.profileId(Role.TEACHER).filter(teacherId::equals).isPresent();
    }

    private static void requireStaffOver(Actor actor, UUID teacherId) {
        if (!staffOver(actor, teacherId)) {
            throw new CourseOutOfReachException();
        }
    }

    private Optional<UUID> enrolledStudentIn(Actor actor, Course course) {
        return actor.profileId(Role.STUDENT)
                .filter(studentId -> enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(
                        course.getId(), studentId, EnrollmentStatus.ACTIVE));
    }

    private Course existing(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private static UUID ownTeacherProfile(Actor actor) {
        return actor.profileId(Role.TEACHER).orElseThrow(TeacherRequiredException::new);
    }

    private static UUID ownStudentProfile(Actor actor) {
        return actor.profileId(Role.STUDENT).orElseThrow(StudentRequiredException::new);
    }

    private static void requireSameTeacher(UUID requestedTeacherId, UUID ownTeacherId) {
        if (requestedTeacherId != null && !requestedTeacherId.equals(ownTeacherId)) {
            throw new CourseOutOfReachException();
        }
    }
}
