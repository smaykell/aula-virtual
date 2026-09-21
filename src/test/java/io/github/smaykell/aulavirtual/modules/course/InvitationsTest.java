package io.github.smaykell.aulavirtual.modules.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvitationsTest {

    private static final String BASE_URL = "https://aula.example/join";

    @Mock
    private CourseRepository courseRepository;

    @Test
    void the_link_hangs_the_code_from_the_configured_base() {
        assertThat(invitationsWith(BASE_URL).of("ABCD2345").url())
                .isEqualTo("https://aula.example/join/ABCD2345");
    }

    @Test
    void a_base_that_ends_in_slashes_does_not_double_the_bar() {
        assertThat(invitationsWith(BASE_URL + "///").of("ABCD2345").url())
                .isEqualTo("https://aula.example/join/ABCD2345");
    }

    @Test
    void a_new_code_is_readable_and_free() {
        when(courseRepository.existsByInvitationCode(anyString())).thenReturn(true, false);

        String code = invitationsWith(BASE_URL).nextCode();

        assertThat(code).hasSize(8).doesNotContainAnyWhitespaces()
                .matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]+");
    }

    private Invitations invitationsWith(String baseUrl) {
        return new Invitations(courseRepository, new CourseProperties(baseUrl));
    }
}
