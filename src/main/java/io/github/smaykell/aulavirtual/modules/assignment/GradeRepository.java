package io.github.smaykell.aulavirtual.modules.assignment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, UUID> {

    Optional<Grade> findBySourceTypeAndSourceId(GradeSource sourceType, UUID sourceId);

    List<Grade> findBySourceTypeAndSourceIdIn(GradeSource sourceType, Collection<UUID> sourceIds);

    List<Grade> findByCourseIdOrderByGradedAtDesc(UUID courseId);

    List<Grade> findByCourseIdAndStudentIdOrderByGradedAtDesc(UUID courseId, UUID studentId);
}
