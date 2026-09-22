package io.github.smaykell.aulavirtual.notification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class LoggingNotificationMailer implements NotificationMailer {

    @Override
    public void send(String recipient, String subject, String body) {
        log.info("Correo no enviado porque no hay SMTP configurado. Para: {} | Asunto: {}\n{}",
                recipient, subject, body);
    }
}
