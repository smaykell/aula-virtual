package io.github.smaykell.aulavirtual.security;

import java.util.Set;
import java.util.UUID;

public record Actor(UUID personId, String username, Set<Role> roles) {

    public boolean canManage(Role target) {
        return roles.stream().anyMatch(role -> role.canManage(target));
    }

    public Set<Role> manageableRoles() {
        return Role.manageableBy(roles);
    }
}
