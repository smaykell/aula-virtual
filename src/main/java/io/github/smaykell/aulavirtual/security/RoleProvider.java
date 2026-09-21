package io.github.smaykell.aulavirtual.security;

import java.util.Optional;
import java.util.UUID;

public interface RoleProvider {

    Optional<Role> activeRoleOf(UUID personId);
}
