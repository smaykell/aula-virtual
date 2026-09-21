package io.github.smaykell.aulavirtual.course.unit;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    List<Material> findByUnitIdOrderByPublishedAt(UUID unitId);

    List<Material> findByUnitIdInOrderByPublishedAt(Collection<UUID> unitIds);

    void deleteByUnitId(UUID unitId);
}
