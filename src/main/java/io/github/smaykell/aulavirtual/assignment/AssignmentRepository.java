package io.github.smaykell.aulavirtual.assignment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByUnitIdOrderByDueAt(UUID unitId);

    List<Assignment> findByCourseIdOrderByDueAt(UUID courseId);

    List<Assignment> findByRemindedAtIsNullAndDueAtBetween(Instant from, Instant to);
}
