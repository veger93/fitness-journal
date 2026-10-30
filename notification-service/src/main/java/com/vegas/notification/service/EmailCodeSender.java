package com.vegas.notification.service;

import com.vegas.common.event.EmailCodeRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Письмо с кодом из 6 цифр (подтверждение email, сброс пароля).
 * В ленту уведомлений не пишем и код в базе не храним: это одноразовый секрет, нужен только в почте.
 */
@Slf4j
@Service
public class EmailCodeSender {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailCodeSender(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(EmailCodeRequestedEvent event) {
        boolean reset = "RESET_PASSWORD".equals(event.purpose());
        String name = event.displayName() == null || event.displayName().isBlank() ? "Привет" : event.displayName();

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(event.email());
        mail.setSubject(reset ? "Vegas: код для смены пароля — " + event.code() : "Vegas: код подтверждения — " + event.code());
        mail.setText(name + ",\n\n"
                + (reset ? "ваш код для смены пароля: " : "ваш код подтверждения email: ") + event.code() + "\n\n"
                + "Код действует 15 минут. Если вы ничего не запрашивали — просто проигнорируйте это письмо.\n\n"
                + "— Vegas, дневник тренировок");
        mailSender.send(mail);
        // сам код в лог не пишем
        log.info("Email code ({}) sent to user {}", event.purpose(), event.userId());
    }
}
