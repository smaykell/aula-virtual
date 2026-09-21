package io.github.smaykell.aulavirtual.modules.course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitRepository extends JpaRepository<Unit, UUID> {

    List<Unit> findByCourseIdOrderByPosition(UUID courseId);

    @Query("select max(u.position) from Unit u where u.courseId = :courseId")
    Optional<Integer> findLastPosition(@Param("courseId") UUID courseId);
}
