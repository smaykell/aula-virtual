package io.github.smaykell.aulavirtual.teacher;

import io.github.smaykell.aulavirtual.security.Profile;
import io.github.smaykell.aulavirtual.security.ProfileProvider;
import io.github.smaykell.aulavirtual.security.Role;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherProfileProvider implements ProfileProvider {

    private final TeacherRepository teacherRepository;

    @Override
    public Optional<Profile> activeProfileOf(UUID personId) {
        return teacherRepository.findByPersonId(personId)
                .filter(Teacher::isActive)
                .map(teacher -> new Profile(Role.TEACHER, teacher.getId()));
    }
}
