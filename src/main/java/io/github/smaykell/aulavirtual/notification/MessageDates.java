package io.github.smaykell.aulavirtual.notification;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class MessageDates {

    private final DateTimeFormatter formatter;

    public MessageDates(NotificationProperties properties) {
        this.formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm")
                .withZone(properties.timeZone());
    }

    public String of(Instant moment) {
        return formatter.format(moment);
    }
}
