package io.github.smaykell.aulavirtual.security;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record Actor(UUID personId, String username, Map<Role, UUID> profiles) {

    public Set<Role> roles() {
        return profiles.keySet();
    }

    public Optional<UUID> profileId(Role role) {
        return Optional.ofNullable(profiles.get(role));
    }

    public boolean canManage(Role target) {
        return roles().stream().anyMatch(role -> role.canManage(target));
    }

    public Set<Role> manageableRoles() {
        return Role.manageableBy(roles());
    }
}
