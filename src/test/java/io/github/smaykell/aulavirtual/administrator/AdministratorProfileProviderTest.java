package io.github.smaykell.aulavirtual.administrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.security.Profile;
import io.github.smaykell.aulavirtual.security.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdministratorProfileProviderTest {

    private static final UUID PERSON = UUID.randomUUID();
    private static final UUID PROFILE = UUID.randomUUID();

    @Mock
    private AdministratorRepository administratorRepository;

    private AdministratorProfileProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AdministratorProfileProvider(administratorRepository);
    }

    @Test
    void the_role_granted_is_the_one_stored_in_the_row_and_not_a_fixed_one() {
        givenTheAdministrator(Role.SUPER_ADMIN, true);

        assertThat(provider.activeProfileOf(PERSON))
                .contains(new Profile(Role.SUPER_ADMIN, PROFILE));
    }

    @Test
    void an_ordinary_administrator_grants_the_admin_role() {
        givenTheAdministrator(Role.ADMIN, true);

        assertThat(provider.activeProfileOf(PERSON))
                .contains(new Profile(Role.ADMIN, PROFILE));
    }

    @Test
    void an_administrator_given_up_grants_nothing() {
        givenTheAdministrator(Role.SUPER_ADMIN, false);

        assertThat(provider.activeProfileOf(PERSON)).isEmpty();
    }

    @Test
    void a_person_without_the_profile_grants_nothing() {
        when(administratorRepository.findByPersonId(PERSON)).thenReturn(Optional.empty());

        assertThat(provider.activeProfileOf(PERSON)).isEmpty();
    }

    private void givenTheAdministrator(Role role, boolean active) {
        Administrator administrator = Administrator.create(PERSON);
        ReflectionTestUtils.setField(administrator, "id", PROFILE);
        ReflectionTestUtils.setField(administrator, "role", role);
        if (!active) {
            administrator.deactivate();
        }
        when(administratorRepository.findByPersonId(PERSON))
                .thenReturn(Optional.of(administrator));
    }
}
