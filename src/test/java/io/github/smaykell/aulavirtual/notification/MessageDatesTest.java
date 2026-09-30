package io.github.smaykell.aulavirtual.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class MessageDatesTest {

    @Test
    void a_moment_is_written_in_the_time_zone_of_the_school() {
        MessageDates dates = new MessageDates(new NotificationProperties(Duration.ofSeconds(30),
                50, 3, Duration.ofMinutes(1), "aula@escuela.pe", ZoneId.of("America/Lima")));

        assertThat(dates.of(Instant.parse("2026-10-13T04:59:00Z")))
                .isEqualTo("12/10/2026 a las 23:59");
    }
}
