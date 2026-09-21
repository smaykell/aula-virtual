package io.github.smaykell.aulavirtual.modules.student;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    Page<Student> findByActive(boolean active, Pageable pageable);

    Optional<Student> findByPersonId(UUID personId);

    boolean existsByPersonId(UUID personId);
}
