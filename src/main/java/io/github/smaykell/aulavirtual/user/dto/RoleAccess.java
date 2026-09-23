package io.github.smaykell.aulavirtual.user.dto;

import io.github.smaykell.aulavirtual.security.Permission;
import io.github.smaykell.aulavirtual.security.Role;
import java.util.Collection;
import java.util.List;

public record RoleAccess(Role role, List<String> permissions) {

    public static List<RoleAccess> of(Collection<Role> roles) {
        return Role.sorted(roles).stream().map(RoleAccess::of).toList();
    }

    private static RoleAccess of(Role role) {
        return new RoleAccess(role,
                role.permissions().stream().map(Permission::authority).sorted().toList());
    }
}
