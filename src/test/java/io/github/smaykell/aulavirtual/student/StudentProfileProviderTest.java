package io.github.smaykell.aulavirtual.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.security.Profile;
import io.github.smaykell.aulavirtual.security.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentProfileProviderTest {

    private static final UUID PERSON = UUID.randomUUID();
    private static final UUID PROFILE = UUID.randomUUID();

    @Mock
    private StudentRepository studentRepository;

    private StudentProfileProvider provider;

    @BeforeEach
    void setUp() {
        provider = new StudentProfileProvider(studentRepository);
    }

    @Test
    void an_active_profile_grants_its_role_and_carries_its_id() {
        givenStudent(true);

        assertThat(provider.activeProfileOf(PERSON))
                .contains(new Profile(Role.STUDENT, PROFILE));
    }

    @Test
    void a_profile_given_up_grants_nothing() {
        givenStudent(false);

        assertThat(provider.activeProfileOf(PERSON)).isEmpty();
    }

    @Test
    void a_person_without_the_profile_grants_nothing() {
        when(studentRepository.findByPersonId(PERSON)).thenReturn(Optional.empty());

        assertThat(provider.activeProfileOf(PERSON)).isEmpty();
    }

    private void givenStudent(boolean active) {
        Student student = Student.create(PERSON);
        ReflectionTestUtils.setField(student, "id", PROFILE);
        if (!active) {
            student.deactivate();
        }
        when(studentRepository.findByPersonId(PERSON)).thenReturn(Optional.of(student));
    }
}
