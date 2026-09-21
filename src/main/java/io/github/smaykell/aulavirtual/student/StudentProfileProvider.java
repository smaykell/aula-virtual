package io.github.smaykell.aulavirtual.student;

import io.github.smaykell.aulavirtual.security.Profile;
import io.github.smaykell.aulavirtual.security.ProfileProvider;
import io.github.smaykell.aulavirtual.security.Role;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentProfileProvider implements ProfileProvider {

    private final StudentRepository studentRepository;

    @Override
    public Optional<Profile> activeProfileOf(UUID personId) {
        return studentRepository.findByPersonId(personId)
                .filter(Student::isActive)
                .map(student -> new Profile(Role.STUDENT, student.getId()));
    }
}
