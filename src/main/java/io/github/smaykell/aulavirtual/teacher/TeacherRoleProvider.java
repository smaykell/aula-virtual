package io.github.smaykell.aulavirtual.teacher;

import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.RoleProvider;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherRoleProvider implements RoleProvider {

    private final TeacherRepository teacherRepository;

    @Override
    public Optional<Role> activeRoleOf(UUID personId) {
        return teacherRepository.findByPersonId(personId)
                .filter(Teacher::isActive)
                .map(teacher -> Role.TEACHER);
    }
}
