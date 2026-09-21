package io.github.smaykell.aulavirtual.student;

import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.RoleProvider;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentRoleProvider implements RoleProvider {

    private final StudentRepository studentRepository;

    @Override
    public Optional<Role> activeRoleOf(UUID personId) {
        return studentRepository.findByPersonId(personId)
                .filter(Student::isActive)
                .map(student -> Role.STUDENT);
    }
}
