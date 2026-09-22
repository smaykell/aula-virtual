package io.github.smaykell.aulavirtual.notification;

interface NotificationMailer {

    void send(String recipient, String subject, String body);
}
