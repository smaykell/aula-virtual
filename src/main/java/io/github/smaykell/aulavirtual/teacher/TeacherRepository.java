package io.github.smaykell.aulavirtual.teacher;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TeacherRepository extends JpaRepository<Teacher, UUID>,
        JpaSpecificationExecutor<Teacher> {

    Optional<Teacher> findByPersonId(UUID personId);

    boolean existsByPersonId(UUID personId);
}
