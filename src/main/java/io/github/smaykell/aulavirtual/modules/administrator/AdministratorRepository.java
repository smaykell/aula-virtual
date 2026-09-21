package io.github.smaykell.aulavirtual.modules.administrator;

import io.github.smaykell.aulavirtual.security.Role;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministratorRepository extends JpaRepository<Administrator, UUID> {

    Page<Administrator> findByRoleIn(Collection<Role> roles, Pageable pageable);

    Page<Administrator> findByRoleInAndActive(Collection<Role> roles, boolean active,
            Pageable pageable);

    Optional<Administrator> findByPersonId(UUID personId);

    boolean existsByPersonId(UUID personId);
}
