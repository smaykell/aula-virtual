package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.GradeResponse;
import io.github.smaykell.aulavirtual.assignment.exception.OwnGradeException;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final CourseService courseService;
    private final UserService userService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<GradeResponse> ofCourse(String actorUsername, UUID courseId) {
        CourseMember member = courseService.memberOf(actorUsername, courseId);
        List<Grade> grades = member.staff()
                ? gradeRepository.findByCourseIdOrderByGradedAtDesc(courseId)
                : gradeRepository.findByCourseIdAndStudentIdOrderByGradedAtDesc(courseId,
                        member.studentId());

        return grades.stream().map(GradeResponse::from).toList();
    }

    Grade record(String actorUsername, GradeSource sourceType, UUID sourceId, UUID studentId,
            UUID courseId, BigDecimal score, String feedback) {

        Actor grader = userService.actor(actorUsername);
        requireSomeoneElse(grader, studentId);
        Grade grade = gradeRepository.findBySourceTypeAndSourceId(sourceType, sourceId)
                .orElseGet(() -> gradeRepository.save(
                        Grade.of(sourceType, sourceId, studentId, courseId)));
        grade.record(score, feedback, grader.personId(), clock.instant());
        return grade;
    }

    Map<UUID, GradeResponse> bySource(GradeSource sourceType, Collection<UUID> sourceIds) {
        return gradeRepository.findBySourceTypeAndSourceIdIn(sourceType, sourceIds).stream()
                .collect(Collectors.toMap(Grade::getSourceId, GradeResponse::from));
    }

    private static void requireSomeoneElse(Actor grader, UUID studentId) {
        if (grader.profileId(Role.STUDENT).filter(studentId::equals).isPresent()) {
            throw new OwnGradeException();
        }
    }
}
