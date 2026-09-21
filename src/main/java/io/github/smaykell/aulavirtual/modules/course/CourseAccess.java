package io.github.smaykell.aulavirtual.modules.course;

import io.github.smaykell.aulavirtual.modules.course.exception.ArchivedCourseException;
import io.github.smaykell.aulavirtual.modules.course.exception.CourseNotFoundException;
import io.github.smaykell.aulavirtual.modules.course.exception.CourseOutOfReachException;
import io.github.smaykell.aulavirtual.modules.course.exception.TeacherRequiredException;
import io.github.smaykell.aulavirtual.modules.teacher.TeacherService;
import io.github.smaykell.aulavirtual.modules.user.UserService;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CourseAccess {

    private final CourseRepository courseRepository;
    private final UserService userService;
    private final TeacherService teacherService;

    Course readable(String actorUsername, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        requireScopeOver(userService.actor(actorUsername), course.getTeacherId());
        return course;
    }

    Course writable(String actorUsername, UUID courseId) {
        Course course = readable(actorUsername, courseId);
        if (course.isArchived()) {
            throw new ArchivedCourseException();
        }
        return course;
    }

    UUID resolveTitular(String actorUsername, UUID requestedTeacherId) {
        Actor actor = userService.actor(actorUsername);
        UUID teacherId = requestedTeacherId == null ? ownProfileOf(actor) : requestedTeacherId;
        requireScopeOver(actor, teacherId);
        teacherService.requireActive(teacherId);
        return teacherId;
    }

    void requireTitular(String actorUsername, UUID teacherId) {
        requireScopeOver(userService.actor(actorUsername), teacherId);
        teacherService.requireActive(teacherId);
    }

    UUID teacherFilterFor(String actorUsername, UUID requestedTeacherId) {
        Actor actor = userService.actor(actorUsername);
        if (actor.canManage(Role.TEACHER)) {
            return requestedTeacherId;
        }
        UUID own = ownProfileOf(actor);
        if (requestedTeacherId != null && !requestedTeacherId.equals(own)) {
            throw new CourseOutOfReachException();
        }
        return own;
    }

    private void requireScopeOver(Actor actor, UUID teacherId) {
        if (actor.canManage(Role.TEACHER)) {
            return;
        }
        if (teacherService.activeProfileIdOf(actor.personId())
                .filter(teacherId::equals).isEmpty()) {
            throw new CourseOutOfReachException();
        }
    }

    private UUID ownProfileOf(Actor actor) {
        return teacherService.activeProfileIdOf(actor.personId())
                .orElseThrow(TeacherRequiredException::new);
    }
}
