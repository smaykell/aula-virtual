package io.github.smaykell.aulavirtual.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PersonRolesTest {

    private static final UUID PERSON = UUID.randomUUID();

    @Test
    void a_person_holds_every_role_whose_profile_is_active() {
        PersonRoles personRoles = new PersonRoles(List.of(
                provider(Role.TEACHER), provider(Role.ADMIN), noProfile()));

        assertThat(personRoles.of(PERSON)).containsExactlyInAnyOrder(Role.TEACHER, Role.ADMIN);
    }

    @Test
    void a_person_without_any_active_profile_holds_no_roles() {
        PersonRoles personRoles = new PersonRoles(List.of(noProfile(), noProfile()));

        assertThat(personRoles.of(PERSON)).isEmpty();
    }

    @Test
    void without_any_module_registered_nobody_holds_roles() {
        assertThat(new PersonRoles(List.of()).of(PERSON)).isEmpty();
    }

    private static RoleProvider provider(Role role) {
        return personId -> Optional.of(role);
    }

    private static RoleProvider noProfile() {
        return personId -> Optional.empty();
    }
}
