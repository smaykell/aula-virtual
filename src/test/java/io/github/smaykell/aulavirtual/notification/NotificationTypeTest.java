package io.github.smaykell.aulavirtual.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NotificationTypeTest {

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void no_notification_reaches_the_reader_with_a_hole_in_it(NotificationType type) {
        Map<String, String> data = everythingItAsksFor(type);

        assertThat(type.subjectFor(data)).doesNotContain("{").doesNotContain("}");
        assertThat(type.bodyFor(data)).doesNotContain("{").doesNotContain("}");
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void every_notification_says_something(NotificationType type) {
        Map<String, String> data = everythingItAsksFor(type);

        assertThat(type.subjectFor(data)).isNotBlank();
        assertThat(type.bodyFor(data)).isNotBlank();
    }

    @Test
    void a_missing_value_is_rejected_instead_of_printed_as_a_placeholder() {
        assertThatThrownBy(() -> NotificationType.ENROLLMENT_ACTIVE
                .bodyFor(Map.of("firstName", "Ana Maria")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ENROLLMENT_ACTIVE");
    }

    @Test
    void the_welcome_tells_the_student_how_to_get_in() {
        String body = NotificationType.ACCOUNT_CREATED.bodyFor(
                Map.of("firstName", "Ana Maria", "username", "45678912"));

        assertThat(body).contains("45678912").contains("documento");
    }

    private static Map<String, String> everythingItAsksFor(NotificationType type) {
        return type.placeholders().stream()
                .collect(Collectors.toMap(name -> name, name -> "valor de " + name));
    }
}
