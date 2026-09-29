package io.github.smaykell.aulavirtual.gradebook;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeCategoryRepository extends JpaRepository<GradeCategory, UUID> {

    List<GradeCategory> findByCourseIdOrderByPosition(UUID courseId);

    boolean existsByIdAndCourseId(UUID id, UUID courseId);
}
