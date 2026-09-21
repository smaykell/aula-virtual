package io.github.smaykell.aulavirtual.teacher;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.teacher.dto.CreateTeacherRequest;
import io.github.smaykell.aulavirtual.teacher.dto.TeacherResponse;
import io.github.smaykell.aulavirtual.teacher.dto.TeacherSummary;
import io.github.smaykell.aulavirtual.teacher.dto.UpdateTeacherRequest;
import io.github.smaykell.aulavirtual.teacher.exception.InactiveTeacherException;
import io.github.smaykell.aulavirtual.teacher.exception.TeacherAlreadyRegisteredException;
import io.github.smaykell.aulavirtual.teacher.exception.TeacherNotFoundException;
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
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final PersonService personService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> list(String actorUsername, Boolean active,
            Pageable pageable) {

        userService.requireManagerOf(actorUsername, Role.TEACHER);
        Page<Teacher> teachers = active == null
                ? teacherRepository.findAll(pageable)
                : teacherRepository.findByActive(active, pageable);

        List<UUID> personIds = teachers.getContent().stream().map(Teacher::getPersonId).toList();
        Map<UUID, PersonResponse> persons = personService.byIds(personIds);
        Map<UUID, String> usernames = userService.usernamesByPersonId(personIds);

        return PageResponse.of(teachers, teacher -> TeacherResponse.from(teacher,
                persons.get(teacher.getPersonId()), usernames.get(teacher.getPersonId())));
    }

    @Transactional(readOnly = true)
    public TeacherResponse get(String actorUsername, UUID teacherId) {
        userService.requireManagerOf(actorUsername, Role.TEACHER);
        return responseFor(existing(teacherId));
    }

    @Transactional
    public TeacherResponse create(String actorUsername, CreateTeacherRequest request) {
        userService.requireManagerOf(actorUsername, Role.TEACHER);

        UUID personId = personService.resolveOrCreate(request.person());
        if (teacherRepository.existsByPersonId(personId)) {
            throw new TeacherAlreadyRegisteredException();
        }
        userService.ensureAccount(personId, request.credentials());

        return responseFor(teacherRepository.save(Teacher.create(personId)));
    }

    @Transactional
    public TeacherResponse update(String actorUsername, UUID teacherId,
            UpdateTeacherRequest request) {

        userService.requireManagerOf(actorUsername, Role.TEACHER);
        Teacher teacher = existing(teacherId);
        PersonResponse person = personService.update(teacher.getPersonId(), request.person());
        return TeacherResponse.from(teacher, person,
                userService.usernameOf(teacher.getPersonId()));
    }

    @Transactional
    public TeacherResponse enable(String actorUsername, UUID teacherId) {
        userService.requireManagerOf(actorUsername, Role.TEACHER);
        Teacher teacher = existing(teacherId);
        teacher.activate();
        return responseFor(teacher);
    }

    @Transactional
    public TeacherResponse disable(String actorUsername, UUID teacherId) {
        userService.requireManagerOf(actorUsername, Role.TEACHER);
        Teacher teacher = existing(teacherId);
        teacher.deactivate();
        return responseFor(teacher);
    }

    @Transactional
    public void changePassword(String actorUsername, UUID teacherId,
            ChangePasswordRequest request) {

        userService.requireManagerOf(actorUsername, Role.TEACHER);
        userService.changePasswordOf(existing(teacherId).getPersonId(), request.password());
    }

    @Transactional(readOnly = true)
    public void requireActive(UUID teacherId) {
        if (!existing(teacherId).isActive()) {
            throw new InactiveTeacherException(teacherId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<UUID> activeProfileIdOf(UUID personId) {
        return teacherRepository.findByPersonId(personId)
                .filter(Teacher::isActive)
                .map(Teacher::getId);
    }

    @Transactional(readOnly = true)
    public TeacherSummary summaryOf(UUID teacherId) {
        Teacher teacher = existing(teacherId);
        return TeacherSummary.from(teacher, personService.get(teacher.getPersonId()));
    }

    @Transactional(readOnly = true)
    public Map<UUID, TeacherSummary> summariesOf(Collection<UUID> teacherIds) {
        List<Teacher> teachers = teacherRepository.findAllById(teacherIds);
        Map<UUID, PersonResponse> persons = personService.byIds(
                teachers.stream().map(Teacher::getPersonId).toList());

        return teachers.stream().collect(Collectors.toMap(Teacher::getId,
                teacher -> TeacherSummary.from(teacher, persons.get(teacher.getPersonId()))));
    }

    private Teacher existing(UUID teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new TeacherNotFoundException(teacherId));
    }

    private TeacherResponse responseFor(Teacher teacher) {
        return TeacherResponse.from(teacher, personService.get(teacher.getPersonId()),
                userService.usernameOf(teacher.getPersonId()));
    }
}
