package io.github.smaykell.aulavirtual.modules.teacher;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.modules.teacher.exception.TeacherNotFoundException;
import io.github.smaykell.aulavirtual.modules.teacher.dto.CreateTeacherRequest;
import io.github.smaykell.aulavirtual.modules.teacher.dto.TeacherResponse;
import io.github.smaykell.aulavirtual.modules.teacher.dto.UpdateTeacherRequest;
import io.github.smaykell.aulavirtual.modules.user.UserService;
import io.github.smaykell.aulavirtual.modules.user.dto.CreateUserRequest;
import io.github.smaykell.aulavirtual.modules.user.dto.UserResponse;
import io.github.smaykell.aulavirtual.security.Role;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> list(String actorUsername, Boolean active,
            Pageable pageable) {

        userService.requireManagerOf(actorUsername, Role.TEACHER);
        Page<Teacher> teachers = active == null
                ? teacherRepository.findAll(pageable)
                : teacherRepository.findByActive(active, pageable);

        Map<UUID, String> usernames = usernamesOf(teachers.getContent());
        return PageResponse.of(teachers,
                teacher -> TeacherResponse.from(teacher, usernames.get(teacher.getUserId())));
    }

    @Transactional(readOnly = true)
    public TeacherResponse get(String actorUsername, UUID teacherId) {
        userService.requireManagerOf(actorUsername, Role.TEACHER);
        return responseFor(existing(teacherId));
    }

    @Transactional
    public TeacherResponse create(String actorUsername, CreateTeacherRequest request) {
        UserResponse account = userService.create(actorUsername, accountFor(request));
        Teacher teacher = teacherRepository.save(Teacher.create(account.id(), request.firstName(),
                request.lastName(), request.birthDate(), request.sex()));
        return TeacherResponse.from(teacher, account.username());
    }

    @Transactional
    public TeacherResponse update(String actorUsername, UUID teacherId,
            UpdateTeacherRequest request) {

        userService.requireManagerOf(actorUsername, Role.TEACHER);
        Teacher teacher = existing(teacherId);
        teacher.updatePersonalData(request.firstName(), request.lastName(), request.birthDate(),
                request.sex());
        return responseFor(teacher);
    }

    @Transactional
    public TeacherResponse enable(String actorUsername, UUID teacherId) {
        Teacher teacher = existing(teacherId);
        UserResponse account = userService.enable(actorUsername, teacher.getUserId());
        teacher.activate();
        return TeacherResponse.from(teacher, account.username());
    }

    @Transactional
    public TeacherResponse disable(String actorUsername, UUID teacherId) {
        Teacher teacher = existing(teacherId);
        UserResponse account = userService.disable(actorUsername, teacher.getUserId());
        teacher.deactivate();
        return TeacherResponse.from(teacher, account.username());
    }

    private Teacher existing(UUID teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new TeacherNotFoundException(teacherId));
    }

    private TeacherResponse responseFor(Teacher teacher) {
        return TeacherResponse.from(teacher, usernamesOf(List.of(teacher)).get(teacher.getUserId()));
    }

    private Map<UUID, String> usernamesOf(List<Teacher> teachers) {
        return userService.usernamesOf(teachers.stream().map(Teacher::getUserId).toList());
    }

    private CreateUserRequest accountFor(CreateTeacherRequest request) {
        return new CreateUserRequest(request.username(), request.password(), Role.TEACHER);
    }
}
