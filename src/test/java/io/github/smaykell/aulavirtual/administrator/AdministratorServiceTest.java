package io.github.smaykell.aulavirtual.administrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.administrator.dto.AdministratorResponse;
import io.github.smaykell.aulavirtual.administrator.dto.CreateAdministratorRequest;
import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.Sex;
import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.UserService;
import io.github.smaykell.aulavirtual.user.dto.Credentials;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdministratorServiceTest {

    private static final UUID PERSON = UUID.randomUUID();
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 20);
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Mock
    private AdministratorRepository administratorRepository;

    @Mock
    private PersonService personService;

    @Mock
    private UserService userService;

    private AdministratorService administratorService;

    @BeforeEach
    void setUp() {
        administratorService = new AdministratorService(administratorRepository, personService,
                userService);
    }

    @Test
    void the_superadmin_registers_an_administrator() {
        givenTheSuperadminActs();
        when(personService.resolveOrCreate(personData())).thenReturn(PERSON);
        when(administratorRepository.existsByPersonId(PERSON)).thenReturn(false);
        givenTheAdministratorIsStored();
        givenTheProfileOf(PERSON, "nuevo.admin");

        AdministratorResponse created = administratorService.create("root",
                new CreateAdministratorRequest(personData(),
                        new Credentials("nuevo.admin", "contrasena")));

        assertThat(created.role()).isEqualTo(Role.ADMIN);
        assertThat(created.username()).isEqualTo("nuevo.admin");
        assertThat(created.active()).isTrue();
        verify(userService).ensureAccount(eq(PERSON), any(Credentials.class));
    }

    @Test
    void a_person_already_registered_as_an_administrator_is_not_registered_twice() {
        givenTheSuperadminActs();
        when(personService.resolveOrCreate(personData())).thenReturn(PERSON);
        when(administratorRepository.existsByPersonId(PERSON)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> administratorService.create(
                "root", new CreateAdministratorRequest(personData(), null)));

        assertThat(error.getCode()).isEqualTo("ADM_ALREADY_REGISTERED");
        verify(administratorRepository, never()).save(any(Administrator.class));
    }

    @Test
    void nobody_disables_a_superadmin() {
        givenTheSuperadminActs();
        Administrator superadmin = givenTheAdministrator(UUID.randomUUID(), Role.SUPER_ADMIN);

        ApiException error = assertThrows(ApiException.class,
                () -> administratorService.disable("root", superadmin.getId()));

        assertThat(error.getCode()).isEqualTo("USR_ROLE_OUT_OF_REACH");
        assertThat(superadmin.isActive()).isTrue();
    }

    @Test
    void the_superadmin_disables_an_administrator() {
        givenTheSuperadminActs();
        Administrator administrator = givenTheAdministrator(UUID.randomUUID(), Role.ADMIN);
        givenTheProfileOf(PERSON, "ana");

        assertThat(administratorService.disable("root", administrator.getId()).active()).isFalse();
    }

    @Test
    void the_listing_only_shows_the_roles_that_the_actor_manages() {
        givenTheSuperadminActs();
        Administrator administrator = Administrator.create(PERSON);
        ReflectionTestUtils.setField(administrator, "id", UUID.randomUUID());
        when(administratorRepository.findByRoleIn(Role.manageableBy(Set.of(Role.SUPER_ADMIN)),
                FIRST_PAGE)).thenReturn(new PageImpl<>(List.of(administrator), FIRST_PAGE, 1));
        when(personService.byIds(List.of(PERSON))).thenReturn(Map.of(PERSON, personResponse()));
        when(userService.usernamesByPersonId(List.of(PERSON))).thenReturn(Map.of(PERSON, "ana"));

        PageResponse<AdministratorResponse> page = administratorService.list("root", null,
                FIRST_PAGE);

        assertThat(page.content()).singleElement()
                .satisfies(found -> assertThat(found.username()).isEqualTo("ana"));
    }

    @Test
    void an_unknown_administrator_is_not_found() {
        givenTheSuperadminActs();
        UUID administratorId = UUID.randomUUID();
        when(administratorRepository.findById(administratorId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> administratorService.get("root", administratorId));

        assertThat(error.getCode()).isEqualTo("ADM_NOT_FOUND");
    }

    private void givenTheSuperadminActs() {
        when(userService.requireManagerOf("root", Role.ADMIN))
                .thenReturn(new Actor(UUID.randomUUID(), "root",
                        Map.of(Role.SUPER_ADMIN, UUID.randomUUID())));
    }

    private void givenTheAdministratorIsStored() {
        when(administratorRepository.save(any(Administrator.class))).thenAnswer(call -> {
            Administrator administrator = call.getArgument(0);
            ReflectionTestUtils.setField(administrator, "id", UUID.randomUUID());
            return administrator;
        });
    }

    private Administrator givenTheAdministrator(UUID administratorId, Role role) {
        Administrator administrator = Administrator.create(PERSON);
        ReflectionTestUtils.setField(administrator, "id", administratorId);
        ReflectionTestUtils.setField(administrator, "role", role);
        when(administratorRepository.findById(administratorId))
                .thenReturn(Optional.of(administrator));
        return administrator;
    }

    private void givenTheProfileOf(UUID personId, String username) {
        when(personService.get(personId)).thenReturn(personResponse());
        when(userService.usernameOf(personId)).thenReturn(username);
    }

    private static PersonData personData() {
        return new PersonData(DocumentType.DNI, "45678912", "Ana Maria", "Lopez Diaz",
                BIRTH_DATE, Sex.FEMALE, null);
    }

    private static PersonResponse personResponse() {
        return new PersonResponse(PERSON, DocumentType.DNI, "45678912", "Ana Maria", "Lopez Diaz",
                BIRTH_DATE, Sex.FEMALE, null);
    }
}
