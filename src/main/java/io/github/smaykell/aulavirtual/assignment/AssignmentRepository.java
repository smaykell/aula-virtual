package io.github.smaykell.aulavirtual.assignment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByUnitIdOrderByDueAt(UUID unitId);
}
