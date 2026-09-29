package io.github.smaykell.aulavirtual.gradebook;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, UUID> {

    Optional<Grade> findBySourceTypeAndSourceIdAndStudentId(GradeSource sourceType,
            UUID sourceId, UUID studentId);

    List<Grade> findBySourceTypeAndSourceIdAndStudentIdIn(GradeSource sourceType, UUID sourceId,
            Collection<UUID> studentIds);

    boolean existsBySourceTypeAndSourceId(GradeSource sourceType, UUID sourceId);

    List<Grade> findByCourseIdOrderByGradedAtDesc(UUID courseId);

    List<Grade> findByCourseIdAndStudentIdOrderByGradedAtDesc(UUID courseId, UUID studentId);
}
