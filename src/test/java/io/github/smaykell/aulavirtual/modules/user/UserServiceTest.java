package io.github.smaykell.aulavirtual.modules.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.modules.user.dto.CreateUserRequest;
import io.github.smaykell.aulavirtual.modules.user.dto.UserResponse;
import io.github.smaykell.aulavirtual.security.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, reversingEncoder());
    }

    @Test
    void an_admin_creates_a_teacher() {
        givenActor("ana", Role.ADMIN);
        when(userRepository.existsByUsername("nuevo")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserResponse created = userService.create("ana",
                new CreateUserRequest("nuevo", "contrasena", Role.TEACHER));

        assertThat(created.username()).isEqualTo("nuevo");
        assertThat(created.role()).isEqualTo(Role.TEACHER);
        assertThat(created.active()).isTrue();
    }

    @Test
    void an_admin_cannot_create_another_admin() {
        givenActor("ana", Role.ADMIN);

        ApiException error = assertThrows(ApiException.class, () -> userService.create("ana",
                new CreateUserRequest("otro", "contrasena", Role.ADMIN)));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void the_superadmin_creates_admins() {
        givenActor("root", Role.SUPER_ADMIN);
        when(userRepository.existsByUsername("nuevo")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserResponse created = userService.create("root",
                new CreateUserRequest("nuevo", "contrasena", Role.ADMIN));

        assertThat(created.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void nobody_creates_a_superadmin() {
        givenActor("root", Role.SUPER_ADMIN);

        ApiException error = assertThrows(ApiException.class, () -> userService.create("root",
                new CreateUserRequest("otroroot", "contrasena", Role.SUPER_ADMIN)));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void a_deactivated_admin_cannot_create_users() {
        User admin = User.create("ana", "hash", Role.ADMIN);
        admin.deactivate();
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(admin));

        ApiException error = assertThrows(ApiException.class, () -> userService.create("ana",
                new CreateUserRequest("nuevo", "contrasena", Role.TEACHER)));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void a_repeated_username_is_a_conflict() {
        givenActor("ana", Role.ADMIN);
        when(userRepository.existsByUsername("repetido")).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> userService.create("ana",
                new CreateUserRequest("Repetido", "contrasena", Role.STUDENT)));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void the_username_is_normalized_before_being_stored() {
        givenActor("ana", Role.ADMIN);
        when(userRepository.existsByUsername("mayusculas")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        userService.create("ana",
                new CreateUserRequest("  MAYUSCULAS ", "contrasena", Role.STUDENT));

        assertThat(savedUser().getUsername()).isEqualTo("mayusculas");
    }

    @Test
    void the_password_is_encoded_before_being_stored() {
        givenActor("ana", Role.ADMIN);
        when(userRepository.existsByUsername("nuevo")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        userService.create("ana", new CreateUserRequest("nuevo", "contrasena", Role.STUDENT));

        assertThat(savedUser().getPasswordHash()).isEqualTo("anesartnoc");
    }

    @Test
    void an_admin_only_lists_the_roles_it_manages() {
        givenActor("ana", Role.ADMIN);
        when(userRepository.findByRoleIn(any(), eq(FIRST_PAGE))).thenReturn(emptyPage());

        userService.list("ana", null, FIRST_PAGE);

        assertThat(listedRoles()).containsExactlyInAnyOrder(Role.TEACHER, Role.STUDENT);
    }

    @Test
    void the_superadmin_lists_admins_but_never_other_superadmins() {
        givenActor("root", Role.SUPER_ADMIN);
        when(userRepository.findByRoleIn(any(), eq(FIRST_PAGE))).thenReturn(emptyPage());

        userService.list("root", null, FIRST_PAGE);

        assertThat(listedRoles())
                .containsExactlyInAnyOrder(Role.ADMIN, Role.TEACHER, Role.STUDENT);
    }

    @Test
    void the_active_filter_narrows_the_listing() {
        givenActor("ana", Role.ADMIN);
        when(userRepository.findByRoleInAndActive(any(), eq(false), eq(FIRST_PAGE)))
                .thenReturn(emptyPage());

        userService.list("ana", false, FIRST_PAGE);

        verify(userRepository, never()).findByRoleIn(any(), any());
    }

    @Test
    void a_token_of_a_user_that_no_longer_exists_is_rejected() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> userService.list("fantasma", null, FIRST_PAGE));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void givenActor(String username, Role role) {
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(User.create(username, "hash", role)));
    }

    private User savedUser() {
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        return saved.getValue();
    }

    private Collection<Role> listedRoles() {
        ArgumentCaptor<Collection<Role>> roles = ArgumentCaptor.captor();
        verify(userRepository).findByRoleIn(roles.capture(), eq(FIRST_PAGE));
        return roles.getValue();
    }

    private Page<User> emptyPage() {
        return new PageImpl<>(List.of(), FIRST_PAGE, 0);
    }

    private PasswordEncoder reversingEncoder() {
        return new PasswordEncoder() {

            @Override
            public String encode(CharSequence rawPassword) {
                return new StringBuilder(rawPassword).reverse().toString();
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encode(rawPassword).equals(encodedPassword);
            }
        };
    }
}
