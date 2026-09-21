package io.github.smaykell.aulavirtual.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PersonProfilesTest {

    private static final UUID PERSON = UUID.randomUUID();
    private static final UUID TEACHER_PROFILE = UUID.randomUUID();
    private static final UUID ADMIN_PROFILE = UUID.randomUUID();

    @Test
    void a_person_holds_every_role_whose_profile_is_active() {
        PersonProfiles personProfiles = new PersonProfiles(List.of(
                provider(Role.TEACHER, TEACHER_PROFILE),
                provider(Role.ADMIN, ADMIN_PROFILE),
                noProfile()));

        assertThat(personProfiles.rolesOf(PERSON))
                .containsExactlyInAnyOrder(Role.TEACHER, Role.ADMIN);
    }

    @Test
    void every_role_carries_the_id_of_the_profile_that_grants_it() {
        PersonProfiles personProfiles = new PersonProfiles(List.of(
                provider(Role.TEACHER, TEACHER_PROFILE),
                provider(Role.ADMIN, ADMIN_PROFILE)));

        assertThat(personProfiles.of(PERSON)).containsOnly(
                entry(Role.TEACHER, TEACHER_PROFILE),
                entry(Role.ADMIN, ADMIN_PROFILE));
    }

    @Test
    void a_person_without_any_active_profile_holds_no_roles() {
        PersonProfiles personProfiles = new PersonProfiles(List.of(noProfile(), noProfile()));

        assertThat(personProfiles.of(PERSON)).isEmpty();
    }

    @Test
    void without_any_module_registered_nobody_holds_roles() {
        assertThat(new PersonProfiles(List.of()).of(PERSON)).isEmpty();
    }

    private static ProfileProvider provider(Role role, UUID profileId) {
        return personId -> Optional.of(new Profile(role, profileId));
    }

    private static ProfileProvider noProfile() {
        return personId -> Optional.empty();
    }
}
