package io.github.smaykell.aulavirtual.modules.teacher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.domain.Sex;
import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.modules.teacher.dto.CreateTeacherRequest;
import io.github.smaykell.aulavirtual.modules.teacher.dto.TeacherResponse;
import io.github.smaykell.aulavirtual.modules.teacher.dto.UpdateTeacherRequest;
import io.github.smaykell.aulavirtual.modules.user.UserService;
import io.github.smaykell.aulavirtual.modules.user.dto.CreateUserRequest;
import io.github.smaykell.aulavirtual.modules.user.dto.UserResponse;
import io.github.smaykell.aulavirtual.modules.user.exception.RoleOutOfReachException;
import io.github.smaykell.aulavirtual.security.Role;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private UserService userService;

    private TeacherService teacherService;

    @BeforeEach
    void setUp() {
        teacherService = new TeacherService(teacherRepository, userService);
    }

    @Test
    void registering_a_teacher_returns_its_data_and_its_account() {
        givenTheAccountIsCreatedAs("nuevo.docente");
        givenTheTeacherIsStored();

        TeacherResponse teacher = teacherService.create("ana", requestFor("nuevo.docente"));

        assertThat(teacher.username()).isEqualTo("nuevo.docente");
        assertThat(teacher.userId()).isEqualTo(ACCOUNT_ID);
        assertThat(teacher.firstName()).isEqualTo("Juan Carlos");
        assertThat(teacher.lastName()).isEqualTo("Perez Gomez");
        assertThat(teacher.birthDate()).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(teacher.sex()).isEqualTo(Sex.MALE);
    }

    @Test
    void a_registered_teacher_starts_active() {
        givenTheAccountIsCreatedAs("nuevo.docente");
        givenTheTeacherIsStored();

        assertThat(teacherService.create("ana", requestFor("nuevo.docente")).active()).isTrue();
    }

    @Test
    void the_account_is_created_with_the_teacher_role() {
        givenTheAccountIsCreatedAs("nuevo.docente");
        givenTheTeacherIsStored();

        teacherService.create("ana", requestFor("nuevo.docente"));

        assertThat(requestedAccount().role()).isEqualTo(Role.TEACHER);
        assertThat(requestedAccount().password()).isEqualTo("contrasena");
    }

    @Test
    void the_teacher_points_at_the_account_that_was_created() {
        givenTheAccountIsCreatedAs("nuevo.docente");
        givenTheTeacherIsStored();

        teacherService.create("ana", requestFor("nuevo.docente"));

        assertThat(storedTeacher().getUserId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    void the_surrounding_spaces_of_the_names_are_trimmed() {
        givenTheAccountIsCreatedAs("nuevo.docente");
        givenTheTeacherIsStored();

        teacherService.create("ana", new CreateTeacherRequest("  Juan   Carlos ",
                " Perez  Gomez  ", LocalDate.of(1990, 5, 20), Sex.MALE, "nuevo.docente",
                "contrasena"));

        assertThat(storedTeacher().getFirstName()).isEqualTo("Juan Carlos");
        assertThat(storedTeacher().getLastName()).isEqualTo("Perez Gomez");
    }

    @Test
    void a_rejected_account_leaves_no_teacher_behind() {
        when(userService.create(eq("docente"), any(CreateUserRequest.class)))
                .thenThrow(new RoleOutOfReachException());

        ApiException error = assertThrows(ApiException.class,
                () -> teacherService.create("docente", requestFor("nuevo.docente")));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void disabling_a_teacher_also_turns_its_account_off() {
        Teacher teacher = givenStoredTeacher();
        when(userService.disable("ana", teacher.getUserId())).thenReturn(account("docente", false));

        TeacherResponse disabled = teacherService.disable("ana", teacher.getId());

        assertThat(teacher.isActive()).isFalse();
        assertThat(disabled.active()).isFalse();
        verify(userService).disable("ana", teacher.getUserId());
    }

    @Test
    void enabling_a_teacher_also_turns_its_account_back_on() {
        Teacher teacher = givenStoredTeacher();
        teacher.deactivate();
        when(userService.enable("ana", teacher.getUserId())).thenReturn(account("docente", true));

        assertThat(teacherService.enable("ana", teacher.getId()).active()).isTrue();
        verify(userService).enable("ana", teacher.getUserId());
    }

    @Test
    void a_rejected_disable_leaves_the_teacher_active() {
        Teacher teacher = givenStoredTeacher();
        when(userService.disable("otro", teacher.getUserId()))
                .thenThrow(new RoleOutOfReachException());

        assertThrows(ApiException.class, () -> teacherService.disable("otro", teacher.getId()));

        assertThat(teacher.isActive()).isTrue();
    }

    @Test
    void updating_a_teacher_replaces_its_personal_data() {
        Teacher teacher = givenStoredTeacher();
        givenTheUsernameLookupAnswers(teacher, "docente");

        TeacherResponse updated = teacherService.update("ana", teacher.getId(),
                new UpdateTeacherRequest("  Ana  Maria ", " Lopez ", LocalDate.of(1985, 3, 1),
                        Sex.FEMALE));

        assertThat(updated.firstName()).isEqualTo("Ana Maria");
        assertThat(updated.lastName()).isEqualTo("Lopez");
        assertThat(updated.birthDate()).isEqualTo(LocalDate.of(1985, 3, 1));
        assertThat(updated.sex()).isEqualTo(Sex.FEMALE);
        assertThat(updated.username()).isEqualTo("docente");
    }

    @Test
    void updating_a_teacher_checks_the_actor_first() {
        doThrow(new RoleOutOfReachException())
                .when(userService).requireManagerOf("otro", Role.TEACHER);

        assertThrows(ApiException.class, () -> teacherService.update("otro", UUID.randomUUID(),
                new UpdateTeacherRequest("Ana", "Lopez", LocalDate.of(1985, 3, 1), Sex.FEMALE)));

        verify(teacherRepository, never()).findById(any());
    }

    @Test
    void reading_a_teacher_that_does_not_exist_is_a_404() {
        UUID unknown = UUID.randomUUID();
        when(teacherRepository.findById(unknown)).thenReturn(Optional.empty());

        ApiException error =
                assertThrows(ApiException.class, () -> teacherService.get("ana", unknown));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(error.getCode()).isEqualTo("TCH_NOT_FOUND");
        assertThat(error.getMessage()).isEqualTo("Docente con id %s no encontrado".formatted(unknown));
    }

    @Test
    void the_listing_resolves_the_username_of_every_teacher() {
        Teacher teacher = aTeacher();
        when(teacherRepository.findAll(FIRST_PAGE))
                .thenReturn(new PageImpl<>(List.of(teacher), FIRST_PAGE, 1));
        givenTheUsernameLookupAnswers(teacher, "docente");

        PageResponse<TeacherResponse> page = teacherService.list("ana", null, FIRST_PAGE);

        assertThat(page.content()).singleElement()
                .satisfies(found -> assertThat(found.username()).isEqualTo("docente"));
    }

    @Test
    void the_active_filter_narrows_the_listing() {
        when(teacherRepository.findByActive(eq(false), eq(FIRST_PAGE)))
                .thenReturn(new PageImpl<>(List.of(), FIRST_PAGE, 0));

        teacherService.list("ana", false, FIRST_PAGE);

        verify(teacherRepository, never()).findAll(any(Pageable.class));
    }

    private Teacher givenStoredTeacher() {
        Teacher teacher = aTeacher();
        when(teacherRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        return teacher;
    }

    private Teacher aTeacher() {
        Teacher teacher = Teacher.create(UUID.randomUUID(), "Juan Carlos", "Perez Gomez",
                LocalDate.of(1990, 5, 20), Sex.MALE);
        ReflectionTestUtils.setField(teacher, "id", UUID.randomUUID());
        return teacher;
    }

    private void givenTheUsernameLookupAnswers(Teacher teacher, String username) {
        when(userService.usernamesOf(List.of(teacher.getUserId())))
                .thenReturn(Map.of(teacher.getUserId(), username));
    }

    private UserResponse account(String username, boolean active) {
        return new UserResponse(ACCOUNT_ID, username, Role.TEACHER, active, Instant.EPOCH);
    }

    private CreateTeacherRequest requestFor(String username) {
        return new CreateTeacherRequest("Juan Carlos", "Perez Gomez", LocalDate.of(1990, 5, 20),
                Sex.MALE, username, "contrasena");
    }

    private void givenTheAccountIsCreatedAs(String username) {
        when(userService.create(eq("ana"), any(CreateUserRequest.class))).thenReturn(
                new UserResponse(ACCOUNT_ID, username, Role.TEACHER, true, Instant.EPOCH));
    }

    private void givenTheTeacherIsStored() {
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(call -> call.getArgument(0));
    }

    private CreateUserRequest requestedAccount() {
        ArgumentCaptor<CreateUserRequest> account = ArgumentCaptor.captor();
        verify(userService).create(eq("ana"), account.capture());
        return account.getValue();
    }

    private Teacher storedTeacher() {
        ArgumentCaptor<Teacher> teacher = ArgumentCaptor.captor();
        verify(teacherRepository).save(teacher.capture());
        return teacher.getValue();
    }
}
