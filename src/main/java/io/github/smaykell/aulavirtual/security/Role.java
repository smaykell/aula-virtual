package io.github.smaykell.aulavirtual.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Role {

    SUPER_ADMIN(Set.of(Permission.ADMINISTRATORS_READ, Permission.ADMINISTRATORS_CREATE,
            Permission.ADMINISTRATORS_UPDATE, Permission.TEACHERS_READ, Permission.TEACHERS_CREATE,
            Permission.TEACHERS_UPDATE, Permission.STUDENTS_READ, Permission.STUDENTS_CREATE,
            Permission.STUDENTS_UPDATE, Permission.COURSES_READ, Permission.COURSES_CREATE,
            Permission.COURSES_UPDATE, Permission.ENROLLMENTS_READ,
            Permission.ENROLLMENTS_UPDATE)),
    ADMIN(Set.of(Permission.TEACHERS_READ, Permission.TEACHERS_CREATE,
            Permission.TEACHERS_UPDATE, Permission.STUDENTS_READ, Permission.STUDENTS_CREATE,
            Permission.STUDENTS_UPDATE, Permission.COURSES_READ, Permission.COURSES_CREATE,
            Permission.COURSES_UPDATE, Permission.ENROLLMENTS_READ,
            Permission.ENROLLMENTS_UPDATE)),
    TEACHER(Set.of(Permission.COURSES_READ, Permission.COURSES_CREATE,
            Permission.COURSES_UPDATE, Permission.ENROLLMENTS_READ,
            Permission.ENROLLMENTS_UPDATE)),
    STUDENT(Set.of(Permission.COURSES_READ, Permission.ENROLLMENTS_CREATE));

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

    public static Set<Permission> permissionsOf(Collection<Role> roles) {
        return roles.stream()
                .flatMap(role -> role.permissions().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<Role> manageableBy(Collection<Role> roles) {
        return roles.stream()
                .flatMap(role -> role.manageableRoles().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static List<Role> sorted(Collection<Role> roles) {
        return roles.stream().sorted().toList();
    }

    public static List<String> permissionAuthoritiesOf(Collection<Role> roles) {
        return permissionsOf(roles).stream().map(Permission::authority).sorted().toList();
    }

    public static List<String> grantedAuthoritiesOf(Collection<Role> roles) {
        return Stream.concat(
                roles.stream().map(Role::authority),
                permissionsOf(roles).stream().map(Permission::authority))
                .sorted()
                .toList();
    }
}
