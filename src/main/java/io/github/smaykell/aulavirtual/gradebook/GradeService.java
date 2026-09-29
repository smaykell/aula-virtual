package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeEntry;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.gradebook.exception.OwnGradeException;
import io.github.smaykell.aulavirtual.gradebook.exception.ScoreOutOfRangeException;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final UserService userService;
    private final Clock clock;

    @Transactional
    public GradeResponse record(String actorUsername, GradeEntry entry) {
        if (!entry.withinRange()) {
            throw new ScoreOutOfRangeException(entry.maxScore());
        }
        Actor grader = userService.actor(actorUsername);
        requireSomeoneElse(grader, entry.studentId());

        Grade grade = gradeRepository.findBySourceTypeAndSourceIdAndStudentId(entry.sourceType(),
                        entry.sourceId(), entry.studentId())
                .orElseGet(() -> gradeRepository.save(Grade.of(entry.sourceType(),
                        entry.sourceId(), entry.studentId(), entry.courseId())));
        grade.record(entry.score(), entry.maxScore(), entry.feedback(), grader.personId(),
                clock.instant());
        return GradeResponse.from(grade);
    }

    @Transactional(readOnly = true)
    public Map<UUID, GradeResponse> visibleTo(CourseMember member, GradeSource sourceType,
            UUID sourceId, Collection<UUID> studentIds) {

        return gradeRepository.findBySourceTypeAndSourceIdAndStudentIdIn(sourceType, sourceId,
                        studentIds).stream()
                .filter(shownTo(member))
                .collect(Collectors.toMap(Grade::getStudentId, GradeResponse::from));
    }

    @Transactional
    public List<GradeResponse> handBack(GradeSource sourceType, UUID sourceId,
            Collection<UUID> studentIds) {

        Instant now = clock.instant();
        List<Grade> grades = gradeRepository.findBySourceTypeAndSourceIdAndStudentIdIn(
                sourceType, sourceId, studentIds);
        grades.forEach(grade -> grade.handBack(now));
        return grades.stream().map(GradeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public boolean anyFor(GradeSource sourceType, UUID sourceId) {
        return gradeRepository.existsBySourceTypeAndSourceId(sourceType, sourceId);
    }

    static Predicate<Grade> shownTo(CourseMember member) {
        return grade -> member.staff() || grade.isReturned();
    }

    private static void requireSomeoneElse(Actor grader, UUID studentId) {
        if (grader.profileId(Role.STUDENT).filter(studentId::equals).isPresent()) {
            throw new OwnGradeException();
        }
    }
}
