package io.github.smaykell.aulavirtual.course;

import io.github.smaykell.aulavirtual.course.exception.ArchivedCourseException;
import io.github.smaykell.aulavirtual.course.exception.CourseNotFoundException;
import io.github.smaykell.aulavirtual.course.exception.CourseOutOfReachException;
import io.github.smaykell.aulavirtual.course.exception.StudentRequiredException;
import io.github.smaykell.aulavirtual.course.exception.TeacherRequiredException;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.teacher.TeacherService;
import io.github.smaykell.aulavirtual.user.UserService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CourseAccess {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserService userService;
    private final TeacherService teacherService;
    private final StudentService studentService;

    record Reader(Course course, boolean staff, UUID studentId) {
    }

    record Scope(UUID teacherId, UUID studentId) {

        boolean staff() {
            return studentId == null;
        }
    }

    Reader readable(String actorUsername, UUID courseId) {
        Course course = existing(courseId);
        Actor actor = userService.actor(actorUsername);
        if (staffOver(actor, course.getTeacherId())) {
            return new Reader(course, true, null);
        }
        return new Reader(course, false, enrolledStudentIn(actor, course)
                .orElseThrow(CourseOutOfReachException::new));
    }

    Course managed(String actorUsername, UUID courseId) {
        Course course = existing(courseId);
        requireStaffOver(userService.actor(actorUsername), course.getTeacherId());
        return course;
    }

    Course writable(String actorUsername, UUID courseId) {
        Course course = managed(actorUsername, courseId);
        if (course.isArchived()) {
            throw new ArchivedCourseException();
        }
        return course;
    }

    UUID resolveTitular(String actorUsername, UUID requestedTeacherId) {
        Actor actor = userService.actor(actorUsername);
        UUID teacherId = requestedTeacherId == null ? ownTeacherProfile(actor)
                : requestedTeacherId;
        requireStaffOver(actor, teacherId);
        teacherService.requireActive(teacherId);
        return teacherId;
    }

    void requireTitular(String actorUsername, UUID teacherId) {
        requireStaffOver(userService.actor(actorUsername), teacherId);
        teacherService.requireActive(teacherId);
    }

    Scope listingScope(String actorUsername, UUID requestedTeacherId) {
        Actor actor = userService.actor(actorUsername);
        if (actor.canManage(Role.TEACHER)) {
            return new Scope(requestedTeacherId, null);
        }
        UUID ownTeacher = teacherService.activeProfileIdOf(actor.personId()).orElse(null);
        if (ownTeacher != null) {
            requireSameTeacher(requestedTeacherId, ownTeacher);
            return new Scope(ownTeacher, null);
        }
        return new Scope(null, ownStudentProfile(actor));
    }

    UUID requireStudent(String actorUsername) {
        return ownStudentProfile(userService.actor(actorUsername));
    }

    private boolean staffOver(Actor actor, UUID teacherId) {
        return actor.canManage(Role.TEACHER)
                || teacherService.activeProfileIdOf(actor.personId())
                        .filter(teacherId::equals).isPresent();
    }

    private void requireStaffOver(Actor actor, UUID teacherId) {
        if (!staffOver(actor, teacherId)) {
            throw new CourseOutOfReachException();
        }
    }

    private Optional<UUID> enrolledStudentIn(Actor actor, Course course) {
        return studentService.activeProfileIdOf(actor.personId())
                .filter(studentId -> enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(
                        course.getId(), studentId, EnrollmentStatus.ACTIVE));
    }

    private Course existing(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private UUID ownTeacherProfile(Actor actor) {
        return teacherService.activeProfileIdOf(actor.personId())
                .orElseThrow(TeacherRequiredException::new);
    }

    private UUID ownStudentProfile(Actor actor) {
        return studentService.activeProfileIdOf(actor.personId())
                .orElseThrow(StudentRequiredException::new);
    }

    private static void requireSameTeacher(UUID requestedTeacherId, UUID ownTeacherId) {
        if (requestedTeacherId != null && !requestedTeacherId.equals(ownTeacherId)) {
            throw new CourseOutOfReachException();
        }
    }
}
