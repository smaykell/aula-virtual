package io.github.smaykell.aulavirtual.student;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.student.dto.CreateStudentRequest;
import io.github.smaykell.aulavirtual.student.dto.StudentResponse;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import io.github.smaykell.aulavirtual.student.dto.UpdateStudentRequest;
import io.github.smaykell.aulavirtual.student.exception.InactiveStudentException;
import io.github.smaykell.aulavirtual.student.exception.StudentAlreadyRegisteredException;
import io.github.smaykell.aulavirtual.student.exception.StudentNotFoundException;
import io.github.smaykell.aulavirtual.user.UserService;
import io.github.smaykell.aulavirtual.user.dto.ChangePasswordRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final PersonService personService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> list(String actorUsername, Boolean active,
            Pageable pageable) {

        userService.requireManagerOf(actorUsername, Role.STUDENT);
        Page<Student> students = active == null
                ? studentRepository.findAll(pageable)
                : studentRepository.findByActive(active, pageable);

        List<UUID> personIds = students.getContent().stream().map(Student::getPersonId).toList();
        Map<UUID, PersonResponse> persons = personService.byIds(personIds);
        Map<UUID, String> usernames = userService.usernamesByPersonId(personIds);

        return PageResponse.of(students, student -> StudentResponse.from(student,
                persons.get(student.getPersonId()), usernames.get(student.getPersonId())));
    }

    @Transactional(readOnly = true)
    public StudentResponse get(String actorUsername, UUID studentId) {
        userService.requireManagerOf(actorUsername, Role.STUDENT);
        return responseFor(existing(studentId));
    }

    @Transactional
    public StudentResponse create(String actorUsername, CreateStudentRequest request) {
        userService.requireManagerOf(actorUsername, Role.STUDENT);

        UUID personId = personService.resolveOrCreate(request.person());
        if (studentRepository.existsByPersonId(personId)) {
            throw new StudentAlreadyRegisteredException();
        }
        userService.ensureAccount(personId, request.credentials());

        return responseFor(studentRepository.save(Student.create(personId)));
    }

    @Transactional
    public StudentResponse update(String actorUsername, UUID studentId,
            UpdateStudentRequest request) {

        userService.requireManagerOf(actorUsername, Role.STUDENT);
        Student student = existing(studentId);
        PersonResponse person = personService.update(student.getPersonId(), request.person());
        return StudentResponse.from(student, person,
                userService.usernameOf(student.getPersonId()));
    }

    @Transactional
    public StudentResponse enable(String actorUsername, UUID studentId) {
        userService.requireManagerOf(actorUsername, Role.STUDENT);
        Student student = existing(studentId);
        student.activate();
        return responseFor(student);
    }

    @Transactional
    public StudentResponse disable(String actorUsername, UUID studentId) {
        userService.requireManagerOf(actorUsername, Role.STUDENT);
        Student student = existing(studentId);
        student.deactivate();
        return responseFor(student);
    }

    @Transactional
    public void changePassword(String actorUsername, UUID studentId,
            ChangePasswordRequest request) {

        userService.requireManagerOf(actorUsername, Role.STUDENT);
        userService.changePasswordOf(existing(studentId).getPersonId(), request.password());
    }

    @Transactional(readOnly = true)
    public void requireActive(UUID studentId) {
        if (!existing(studentId).isActive()) {
            throw new InactiveStudentException(studentId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<UUID> activeProfileIdOf(UUID personId) {
        return studentRepository.findByPersonId(personId)
                .filter(Student::isActive)
                .map(Student::getId);
    }

    @Transactional(readOnly = true)
    public StudentSummary summaryOf(UUID studentId) {
        Student student = existing(studentId);
        return StudentSummary.from(student, personService.get(student.getPersonId()));
    }

    @Transactional(readOnly = true)
    public Map<UUID, StudentSummary> summariesOf(Collection<UUID> studentIds) {
        List<Student> students = studentRepository.findAllById(studentIds);
        Map<UUID, PersonResponse> persons = personService.byIds(
                students.stream().map(Student::getPersonId).toList());

        return students.stream().collect(Collectors.toMap(Student::getId,
                student -> StudentSummary.from(student, persons.get(student.getPersonId()))));
    }

    private Student existing(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
    }

    private StudentResponse responseFor(Student student) {
        return StudentResponse.from(student, personService.get(student.getPersonId()),
                userService.usernameOf(student.getPersonId()));
    }
}
