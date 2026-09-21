package io.github.smaykell.aulavirtual.modules.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByPersonId(UUID personId);

    boolean existsByUsername(String username);

    boolean existsByPersonId(UUID personId);

    List<User> findByPersonIdIn(Collection<UUID> personIds);
}
