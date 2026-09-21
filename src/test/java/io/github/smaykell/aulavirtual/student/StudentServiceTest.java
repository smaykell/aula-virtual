package io.github.smaykell.aulavirtual.student;

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
import io.github.smaykell.aulavirtual.student.dto.CreateStudentRequest;
import io.github.smaykell.aulavirtual.student.dto.StudentResponse;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import io.github.smaykell.aulavirtual.student.dto.UpdateStudentRequest;
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
class StudentServiceTest {

    private static final UUID PERSON = UUID.randomUUID();
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 20);
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PersonService personService;

    @Mock
    private UserService userService;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository, personService, userService);
    }

    @Test
    void registering_a_student_resolves_the_person_and_opens_its_account() {
        givenThePersonResolvesTo(PERSON);
        when(studentRepository.existsByPersonId(PERSON)).thenReturn(false);
        givenTheStudentIsStored();
        givenTheProfileOf(PERSON, "nuevo.docente");

        StudentResponse student = studentService.create("ana", requestFor("nuevo.docente"));

        assertThat(student.username()).isEqualTo("nuevo.docente");
        assertThat(student.person().firstName()).isEqualTo("Juan Carlos");
        assertThat(student.active()).isTrue();
        verify(userService).ensureAccount(eq(PERSON), any(Credentials.class));
    }

    @Test
    void a_person_already_registered_as_a_student_is_not_registered_twice() {
        givenThePersonResolvesTo(PERSON);
        when(studentRepository.existsByPersonId(PERSON)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> studentService.create("ana", requestFor("nuevo.docente")));

        assertThat(error.getCode()).isEqualTo("STD_ALREADY_REGISTERED");
        verify(studentRepository, never()).save(any(Student.class));
        verify(userService, never()).ensureAccount(any(), any());
    }

    @Test
    void a_person_that_already_has_an_account_is_registered_without_credentials() {
        givenThePersonResolvesTo(PERSON);
        when(studentRepository.existsByPersonId(PERSON)).thenReturn(false);
        givenTheStudentIsStored();
        givenTheProfileOf(PERSON, "ana.estudiante");

        StudentResponse student = studentService.create("ana",
                new CreateStudentRequest(personData(), null));

        assertThat(student.username()).isEqualTo("ana.estudiante");
        verify(userService).ensureAccount(PERSON, null);
    }

    @Test
    void whoever_does_not_manage_students_cannot_register_one() {
        doesNotManageStudents("docente");

        assertThrows(RoleOutOfReachException.class,
                () -> studentService.create("docente", requestFor("nuevo.docente")));

        verify(personService, never()).resolveOrCreate(any());
    }

    @Test
    void disabling_a_student_only_turns_off_its_teaching_profile() {
        Student student = givenTheStudent(UUID.randomUUID(), true);
        givenTheProfileOf(PERSON, "ana.docente");

        StudentResponse disabled = studentService.disable("ana", student.getId());

        assertThat(disabled.active()).isFalse();
        assertThat(student.isActive()).isFalse();
    }

    @Test
    void enabling_a_student_turns_its_teaching_profile_back_on() {
        Student student = givenTheStudent(UUID.randomUUID(), false);
        givenTheProfileOf(PERSON, "ana.docente");

        assertThat(studentService.enable("ana", student.getId()).active()).isTrue();
    }

    @Test
    void an_unknown_student_is_not_found() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> studentService.disable("ana", studentId));

        assertThat(error.getCode()).isEqualTo("STD_NOT_FOUND");
    }

    @Test
    void updating_a_student_updates_the_person_shared_with_its_other_profiles() {
        Student student = givenTheStudent(UUID.randomUUID(), true);
        when(personService.update(PERSON, personData())).thenReturn(personResponse());
        when(userService.usernameOf(PERSON)).thenReturn("ana.docente");

        StudentResponse updated = studentService.update("ana", student.getId(),
                new UpdateStudentRequest(personData()));

        assertThat(updated.person().lastName()).isEqualTo("Perez Gomez");
        verify(personService).update(PERSON, personData());
    }

    @Test
    void changing_the_password_of_a_student_changes_the_one_of_its_person() {
        Student student = givenTheStudent(UUID.randomUUID(), true);

        studentService.changePassword("ana", student.getId(),
                new ChangePasswordRequest("contrasena-nueva"));

        verify(userService).changePasswordOf(PERSON, "contrasena-nueva");
    }

    @Test
    void the_listing_joins_each_student_with_its_person_and_its_username() {
        Student student = Student.create(PERSON);
        ReflectionTestUtils.setField(student, "id", UUID.randomUUID());
        when(studentRepository.findAll(FIRST_PAGE))
                .thenReturn(new PageImpl<>(List.of(student), FIRST_PAGE, 1));
        when(personService.byIds(List.of(PERSON))).thenReturn(Map.of(PERSON, personResponse()));
        when(userService.usernamesByPersonId(List.of(PERSON)))
                .thenReturn(Map.of(PERSON, "ana.docente"));

        PageResponse<StudentResponse> page = studentService.list("ana", null, FIRST_PAGE);

        assertThat(page.content()).singleElement().satisfies(found -> {
            assertThat(found.username()).isEqualTo("ana.docente");
            assertThat(found.person().documentNumber()).isEqualTo("45678912");
        });
    }

    @Test
    void a_student_given_up_is_no_longer_active_for_the_rest_of_the_modules() {
        Student student = givenTheStudent(UUID.randomUUID(), false);

        ApiException error = assertThrows(ApiException.class,
                () -> studentService.requireActive(student.getId()));

        assertThat(error.getCode()).isEqualTo("STD_INACTIVE");
    }

    @Test
    void the_summary_of_a_student_carries_its_name_and_nothing_else() {
        Student student = givenTheStudent(UUID.randomUUID(), true);
        when(personService.get(PERSON)).thenReturn(personResponse());

        StudentSummary summary = studentService.summaryOf(student.getId());

        assertThat(summary.id()).isEqualTo(student.getId());
        assertThat(summary.firstName()).isEqualTo("Juan Carlos");
        assertThat(summary.active()).isTrue();
    }

    private void givenThePersonResolvesTo(UUID personId) {
        when(personService.resolveOrCreate(personData())).thenReturn(personId);
    }

    private void givenTheStudentIsStored() {
        when(studentRepository.save(any(Student.class))).thenAnswer(call -> {
            Student student = call.getArgument(0);
            ReflectionTestUtils.setField(student, "id", UUID.randomUUID());
            return student;
        });
    }

    private Student givenTheStudent(UUID studentId, boolean active) {
        Student student = Student.create(PERSON);
        ReflectionTestUtils.setField(student, "id", studentId);
        if (!active) {
            student.deactivate();
        }
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        return student;
    }

    private void givenTheProfileOf(UUID personId, String username) {
        when(personService.get(personId)).thenReturn(personResponse());
        when(userService.usernameOf(personId)).thenReturn(username);
    }

    private void doesNotManageStudents(String actorUsername) {
        when(userService.requireManagerOf(actorUsername, Role.STUDENT))
                .thenThrow(new RoleOutOfReachException());
    }

    private static CreateStudentRequest requestFor(String username) {
        return new CreateStudentRequest(personData(),
                new Credentials(username, "contrasena"));
    }

    private static PersonData personData() {
        return new PersonData(DocumentType.DNI, "45678912", "Juan Carlos", "Perez Gomez",
                BIRTH_DATE, Sex.MALE);
    }

    private static PersonResponse personResponse() {
        return new PersonResponse(PERSON, DocumentType.DNI, "45678912", "Juan Carlos",
                "Perez Gomez", BIRTH_DATE, Sex.MALE);
    }
}
