package io.github.smaykell.aulavirtual.modules.user;

import io.github.smaykell.aulavirtual.security.Role;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<User> findByRoleIn(Collection<Role> roles, Pageable pageable);

    Page<User> findByRoleInAndActive(Collection<Role> roles, boolean active, Pageable pageable);
}
