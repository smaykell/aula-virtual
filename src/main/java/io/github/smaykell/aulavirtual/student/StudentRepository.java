package io.github.smaykell.aulavirtual.student;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudentRepository extends JpaRepository<Student, UUID>,
        JpaSpecificationExecutor<Student> {

    Optional<Student> findByPersonId(UUID personId);

    boolean existsByPersonId(UUID personId);
}
