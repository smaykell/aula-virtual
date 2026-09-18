package io.github.smaykell.aulavirtual.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void the_superadmin_is_the_only_one_that_manages_admins() {
        assertThat(Role.SUPER_ADMIN.canManage(Role.ADMIN)).isTrue();
        assertThat(Role.ADMIN.canManage(Role.ADMIN)).isFalse();
        assertThat(Role.TEACHER.canManage(Role.ADMIN)).isFalse();
        assertThat(Role.STUDENT.canManage(Role.ADMIN)).isFalse();
    }

    @Test
    void nobody_manages_a_superadmin() {
        for (Role role : Role.values()) {
            assertThat(role.canManage(Role.SUPER_ADMIN)).isFalse();
        }
    }

    @Test
    void an_admin_manages_teachers_and_students() {
        assertThat(Role.ADMIN.manageableRoles()).containsExactlyInAnyOrder(Role.TEACHER, Role.STUDENT);
    }

    @Test
    void teachers_and_students_manage_nobody() {
        assertThat(Role.TEACHER.manageableRoles()).isEmpty();
        assertThat(Role.STUDENT.manageableRoles()).isEmpty();
    }

    @Test
    void the_authorities_carry_the_role_prefixed_and_its_permissions() {
        assertThat(Role.ADMIN.grantedAuthorities())
                .containsExactlyInAnyOrder("ROLE_ADMIN", "users:read", "users:create",
                        "users:update", "teachers:read", "teachers:create", "teachers:update");
    }

    @Test
    void a_role_without_permissions_only_carries_its_own_authority() {
        assertThat(Role.STUDENT.grantedAuthorities()).containsExactly("ROLE_STUDENT");
    }
}
