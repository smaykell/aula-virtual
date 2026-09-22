package io.github.smaykell.aulavirtual.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
class NotificationMailerConfig {

    // Las dos implementaciones son @Bean de la misma clase y no @Component porque
    // @ConditionalOnMissingBean solo es determinista en ese orden de declaracion.
    @Bean
    @ConditionalOnProperty(name = "spring.mail.host")
    NotificationMailer smtpNotificationMailer(JavaMailSender mailSender,
            NotificationProperties properties) {

        return new SmtpNotificationMailer(mailSender, properties);
    }

    @Bean
    @ConditionalOnMissingBean(NotificationMailer.class)
    NotificationMailer loggingNotificationMailer() {
        return new LoggingNotificationMailer();
    }
}
