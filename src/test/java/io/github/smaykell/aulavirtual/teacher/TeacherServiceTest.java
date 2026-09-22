package io.github.smaykell.aulavirtual.teacher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.Sex;
import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.teacher.dto.CreateTeacherRequest;
import io.github.smaykell.aulavirtual.teacher.dto.TeacherResponse;
import io.github.smaykell.aulavirtual.teacher.dto.TeacherSummary;
import io.github.smaykell.aulavirtual.teacher.dto.UpdateTeacherRequest;
import io.github.smaykell.aulavirtual.user.UserService;
import io.github.smaykell.aulavirtual.user.dto.ChangePasswordRequest;
import io.github.smaykell.aulavirtual.user.dto.Credentials;
import io.github.smaykell.aulavirtual.user.exception.RoleOutOfReachException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
class TeacherServiceTest {

    private static final UUID PERSON = UUID.randomUUID();
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 20);
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private PersonService personService;

    @Mock
    private UserService userService;

    private TeacherService teacherService;

    @BeforeEach
    void setUp() {
        teacherService = new TeacherService(teacherRepository, personService, userService);
    }

    @Test
    void registering_a_teacher_resolves_the_person_and_opens_its_account() {
        givenThePersonResolvesTo(PERSON);
        when(teacherRepository.existsByPersonId(PERSON)).thenReturn(false);
        givenTheTeacherIsStored();
        givenTheProfileOf(PERSON, "nuevo.docente");

        TeacherResponse teacher = teacherService.create("ana", requestFor("nuevo.docente"));

        assertThat(teacher.username()).isEqualTo("nuevo.docente");
        assertThat(teacher.person().firstName()).isEqualTo("Juan Carlos");
        assertThat(teacher.active()).isTrue();
        verify(userService).ensureAccount(eq(PERSON), any(Credentials.class));
    }

    @Test
    void a_person_already_registered_as_a_teacher_is_not_registered_twice() {
        givenThePersonResolvesTo(PERSON);
        when(teacherRepository.existsByPersonId(PERSON)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> teacherService.create("ana", requestFor("nuevo.docente")));

        assertThat(error.getCode()).isEqualTo("TCH_ALREADY_REGISTERED");
        verify(teacherRepository, never()).save(any(Teacher.class));
        verify(userService, never()).ensureAccount(any(), any());
    }

    @Test
    void a_person_that_already_has_an_account_is_registered_without_credentials() {
        givenThePersonResolvesTo(PERSON);
        when(teacherRepository.existsByPersonId(PERSON)).thenReturn(false);
        givenTheTeacherIsStored();
        givenTheProfileOf(PERSON, "ana.estudiante");

        TeacherResponse teacher = teacherService.create("ana",
                new CreateTeacherRequest(personData(), null));

        assertThat(teacher.username()).isEqualTo("ana.estudiante");
        verify(userService).ensureAccount(PERSON, null);
    }

    @Test
    void whoever_does_not_manage_teachers_cannot_register_one() {
        doesNotManageTeachers("docente");

        assertThrows(RoleOutOfReachException.class,
                () -> teacherService.create("docente", requestFor("nuevo.docente")));

        verify(personService, never()).resolveOrCreate(any());
    }

    @Test
    void disabling_a_teacher_only_turns_off_its_teaching_profile() {
        Teacher teacher = givenTheTeacher(UUID.randomUUID(), true);
        givenTheProfileOf(PERSON, "ana.docente");

        TeacherResponse disabled = teacherService.disable("ana", teacher.getId());

        assertThat(disabled.active()).isFalse();
        assertThat(teacher.isActive()).isFalse();
    }

    @Test
    void enabling_a_teacher_turns_its_teaching_profile_back_on() {
        Teacher teacher = givenTheTeacher(UUID.randomUUID(), false);
        givenTheProfileOf(PERSON, "ana.docente");

        assertThat(teacherService.enable("ana", teacher.getId()).active()).isTrue();
    }

    @Test
    void an_unknown_teacher_is_not_found() {
        UUID teacherId = UUID.randomUUID();
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> teacherService.disable("ana", teacherId));

        assertThat(error.getCode()).isEqualTo("TCH_NOT_FOUND");
    }

    @Test
    void updating_a_teacher_updates_the_person_shared_with_its_other_profiles() {
        Teacher teacher = givenTheTeacher(UUID.randomUUID(), true);
        when(personService.update(PERSON, personData())).thenReturn(personResponse());
        when(userService.usernameOf(PERSON)).thenReturn("ana.docente");

        TeacherResponse updated = teacherService.update("ana", teacher.getId(),
                new UpdateTeacherRequest(personData()));

        assertThat(updated.person().lastName()).isEqualTo("Perez Gomez");
        verify(personService).update(PERSON, personData());
    }

    @Test
    void changing_the_password_of_a_teacher_changes_the_one_of_its_person() {
        Teacher teacher = givenTheTeacher(UUID.randomUUID(), true);

        teacherService.changePassword("ana", teacher.getId(),
                new ChangePasswordRequest("contrasena-nueva"));

        verify(userService).changePasswordOf(PERSON, "contrasena-nueva");
    }

    @Test
    void the_listing_joins_each_teacher_with_its_person_and_its_username() {
        Teacher teacher = Teacher.create(PERSON);
        ReflectionTestUtils.setField(teacher, "id", UUID.randomUUID());
        when(teacherRepository.findAll(FIRST_PAGE))
                .thenReturn(new PageImpl<>(List.of(teacher), FIRST_PAGE, 1));
        when(personService.byIds(List.of(PERSON))).thenReturn(Map.of(PERSON, personResponse()));
        when(userService.usernamesByPersonId(List.of(PERSON)))
                .thenReturn(Map.of(PERSON, "ana.docente"));

        PageResponse<TeacherResponse> page = teacherService.list("ana", null, FIRST_PAGE);

        assertThat(page.content()).singleElement().satisfies(found -> {
            assertThat(found.username()).isEqualTo("ana.docente");
            assertThat(found.person().documentNumber()).isEqualTo("45678912");
        });
    }

    @Test
    void a_teacher_given_up_is_no_longer_active_for_the_rest_of_the_modules() {
        Teacher teacher = givenTheTeacher(UUID.randomUUID(), false);

        ApiException error = assertThrows(ApiException.class,
                () -> teacherService.requireActive(teacher.getId()));

        assertThat(error.getCode()).isEqualTo("TCH_INACTIVE");
    }

    @Test
    void the_summary_of_a_teacher_carries_its_name_and_nothing_else() {
        Teacher teacher = givenTheTeacher(UUID.randomUUID(), true);
        when(personService.get(PERSON)).thenReturn(personResponse());

        TeacherSummary summary = teacherService.summaryOf(teacher.getId());

        assertThat(summary.id()).isEqualTo(teacher.getId());
        assertThat(summary.firstName()).isEqualTo("Juan Carlos");
        assertThat(summary.active()).isTrue();
    }

    private void givenThePersonResolvesTo(UUID personId) {
        when(personService.resolveOrCreate(personData())).thenReturn(personId);
    }

    private void givenTheTeacherIsStored() {
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(call -> {
            Teacher teacher = call.getArgument(0);
            ReflectionTestUtils.setField(teacher, "id", UUID.randomUUID());
            return teacher;
        });
    }

    private Teacher givenTheTeacher(UUID teacherId, boolean active) {
        Teacher teacher = Teacher.create(PERSON);
        ReflectionTestUtils.setField(teacher, "id", teacherId);
        if (!active) {
            teacher.deactivate();
        }
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        return teacher;
    }

    private void givenTheProfileOf(UUID personId, String username) {
        when(personService.get(personId)).thenReturn(personResponse());
        when(userService.usernameOf(personId)).thenReturn(username);
    }

    private void doesNotManageTeachers(String actorUsername) {
        when(userService.requireManagerOf(actorUsername, Role.TEACHER))
                .thenThrow(new RoleOutOfReachException());
    }

    private static CreateTeacherRequest requestFor(String username) {
        return new CreateTeacherRequest(personData(),
                new Credentials(username, "contrasena"));
    }

    private static PersonData personData() {
        return new PersonData(DocumentType.DNI, "45678912", "Juan Carlos", "Perez Gomez",
                BIRTH_DATE, Sex.MALE, null);
    }

    private static PersonResponse personResponse() {
        return new PersonResponse(PERSON, DocumentType.DNI, "45678912", "Juan Carlos",
                "Perez Gomez", BIRTH_DATE, Sex.MALE, null);
    }
}
