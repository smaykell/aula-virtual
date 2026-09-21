package io.github.smaykell.aulavirtual.modules.teacher;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    Page<Teacher> findByActive(boolean active, Pageable pageable);

    Optional<Teacher> findByPersonId(UUID personId);

    boolean existsByPersonId(UUID personId);
}
