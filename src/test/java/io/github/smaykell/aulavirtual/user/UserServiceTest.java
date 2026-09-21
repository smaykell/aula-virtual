package io.github.smaykell.aulavirtual.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.PersonRoles;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.dto.ChangeMyPasswordRequest;
import io.github.smaykell.aulavirtual.user.dto.Credentials;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID PERSON = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonRoles personRoles;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, encoder(), personRoles);
    }

    @Test
    void the_actor_carries_every_role_of_its_person() {
        givenTheAccount("ana", PERSON);
        when(personRoles.of(PERSON)).thenReturn(Set.of(Role.ADMIN, Role.TEACHER));

        Actor actor = userService.actor("ana");

        assertThat(actor.username()).isEqualTo("ana");
        assertThat(actor.personId()).isEqualTo(PERSON);
        assertThat(actor.roles()).containsExactlyInAnyOrder(Role.ADMIN, Role.TEACHER);
    }

    @Test
    void an_unknown_username_is_not_a_valid_actor() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class, () -> userService.actor("fantasma"));

        assertThat(error.getCode()).isEqualTo("USR_UNKNOWN_ACTOR");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void an_account_without_any_active_profile_cannot_act() {
        givenTheAccount("ana", PERSON);
        when(personRoles.of(PERSON)).thenReturn(Set.of());

        ApiException error = assertThrows(ApiException.class, () -> userService.actor("ana"));

        assertThat(error.getCode()).isEqualTo("USR_INACTIVE_ACTOR");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void an_admin_reaches_teachers() {
        givenTheAccount("ana", PERSON);
        when(personRoles.of(PERSON)).thenReturn(Set.of(Role.ADMIN));

        assertThat(userService.requireManagerOf("ana", Role.TEACHER).roles())
                .containsExactly(Role.ADMIN);
    }

    @Test
    void an_admin_does_not_reach_other_admins() {
        givenTheAccount("ana", PERSON);
        when(personRoles.of(PERSON)).thenReturn(Set.of(Role.ADMIN));

        ApiException error = assertThrows(ApiException.class,
                () -> userService.requireManagerOf("ana", Role.ADMIN));

        assertThat(error.getCode()).isEqualTo("USR_ROLE_OUT_OF_REACH");
    }

    @Test
    void a_person_that_is_admin_and_teacher_still_reaches_teachers() {
        givenTheAccount("ana", PERSON);
        when(personRoles.of(PERSON)).thenReturn(Set.of(Role.ADMIN, Role.TEACHER));

        assertThat(userService.requireManagerOf("ana", Role.TEACHER)).isNotNull();
    }

    @Test
    void a_person_without_an_account_gets_one() {
        when(userRepository.existsByPersonId(PERSON)).thenReturn(false);
        when(userRepository.existsByUsername("nuevo.docente")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        userService.ensureAccount(PERSON, new Credentials("Nuevo.Docente", "contrasena"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void a_person_that_already_has_an_account_keeps_it() {
        when(userRepository.existsByPersonId(PERSON)).thenReturn(true);

        userService.ensureAccount(PERSON, null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void sending_credentials_for_a_person_that_already_has_an_account_is_rejected() {
        when(userRepository.existsByPersonId(PERSON)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> userService.ensureAccount(
                PERSON, new Credentials("otro.usuario", "contrasena")));

        assertThat(error.getCode()).isEqualTo("USR_ACCOUNT_ALREADY_EXISTS");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void a_person_without_an_account_and_without_credentials_is_rejected() {
        when(userRepository.existsByPersonId(PERSON)).thenReturn(false);

        ApiException error = assertThrows(ApiException.class,
                () -> userService.ensureAccount(PERSON, null));

        assertThat(error.getCode()).isEqualTo("USR_CREDENTIALS_REQUIRED");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void a_repeated_username_is_rejected() {
        when(userRepository.existsByPersonId(PERSON)).thenReturn(false);
        when(userRepository.existsByUsername("ana")).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> userService.ensureAccount(
                PERSON, new Credentials("ana", "contrasena")));

        assertThat(error.getCode()).isEqualTo("USR_USERNAME_TAKEN");
    }

    @Test
    void changing_my_password_requires_the_current_one() {
        givenTheAccount("ana", PERSON);

        ApiException error = assertThrows(ApiException.class,
                () -> userService.changeOwnPassword("ana",
                        new ChangeMyPasswordRequest("otra", "contrasena-nueva")));

        assertThat(error.getCode()).isEqualTo("USR_CURRENT_PASSWORD_MISMATCH");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changing_my_password_with_the_current_one_replaces_the_hash() {
        User account = givenTheAccount("ana", PERSON);

        userService.changeOwnPassword("ana",
                new ChangeMyPasswordRequest("contrasena", "contrasena-nueva"));

        assertThat(account.getPasswordHash()).isEqualTo("enc:contrasena-nueva");
    }

    @Test
    void an_administrator_replaces_the_password_of_a_person() {
        User account = givenTheAccountOfPerson(PERSON);

        userService.changePasswordOf(PERSON, "contrasena-nueva");

        assertThat(account.getPasswordHash()).isEqualTo("enc:contrasena-nueva");
    }

    @Test
    void a_person_without_an_account_has_no_password_to_change() {
        when(userRepository.findByPersonId(PERSON)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> userService.changePasswordOf(PERSON, "contrasena-nueva"));

        assertThat(error.getCode()).isEqualTo("USR_ACCOUNT_NOT_FOUND");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void the_usernames_come_back_indexed_by_person() {
        UUID otherPerson = UUID.randomUUID();
        when(userRepository.findByPersonIdIn(List.of(PERSON, otherPerson)))
                .thenReturn(List.of(accountOf(PERSON, "ana"), accountOf(otherPerson, "luis")));

        assertThat(userService.usernamesByPersonId(List.of(PERSON, otherPerson)))
                .containsOnly(entry(PERSON, "ana"), entry(otherPerson, "luis"));
    }

    private User givenTheAccount(String username, UUID personId) {
        User account = accountOf(personId, username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(account));
        return account;
    }

    private User givenTheAccountOfPerson(UUID personId) {
        User account = accountOf(personId, "ana");
        when(userRepository.findByPersonId(personId)).thenReturn(Optional.of(account));
        return account;
    }

    private static User accountOf(UUID personId, String username) {
        User account = User.create(personId, username, "enc:contrasena");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        return account;
    }

    private static PasswordEncoder encoder() {
        return new PasswordEncoder() {

            @Override
            public String encode(CharSequence rawPassword) {
                return "enc:" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encode(rawPassword).equals(encodedPassword);
            }
        };
    }
}
