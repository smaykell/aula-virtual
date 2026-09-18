package io.github.smaykell.aulavirtual.security;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public enum Role {

    SUPER_ADMIN(Set.of(Permission.USERS_READ, Permission.USERS_CREATE, Permission.USERS_UPDATE,
            Permission.TEACHERS_READ, Permission.TEACHERS_CREATE, Permission.TEACHERS_UPDATE)),
    ADMIN(Set.of(Permission.USERS_READ, Permission.USERS_CREATE, Permission.USERS_UPDATE,
            Permission.TEACHERS_READ, Permission.TEACHERS_CREATE, Permission.TEACHERS_UPDATE)),
    TEACHER(Set.of()),
    STUDENT(Set.of());

    private static final String AUTHORITY_PREFIX = "ROLE_";

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> permissions() {
        return permissions;
    }

    public String authority() {
        return AUTHORITY_PREFIX + name();
    }

    public List<String> grantedAuthorities() {
        return Stream.concat(Stream.of(authority()), permissions.stream().map(Permission::authority))
                .toList();
    }

    public Set<Role> manageableRoles() {
        return switch (this) {
            case SUPER_ADMIN -> Set.of(ADMIN, TEACHER, STUDENT);
            case ADMIN -> Set.of(TEACHER, STUDENT);
            case TEACHER, STUDENT -> Set.of();
        };
    }

    public boolean canManage(Role target) {
        return manageableRoles().contains(target);
    }
}
