package io.github.smaykell.aulavirtual.administrator;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdministratorRepository extends JpaRepository<Administrator, UUID>,
        JpaSpecificationExecutor<Administrator> {

    Optional<Administrator> findByPersonId(UUID personId);

    boolean existsByPersonId(UUID personId);
}
