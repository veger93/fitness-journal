package com.vegas.notification.service;

import com.vegas.common.event.EmailCodeRequestedEvent;
import com.vegas.notification.entity.EmailMessage;
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
    /** Адрес веб-приложения — ссылка "Открыть дневник" в приветственном письме */
    private final String webUrl;

    public EmailCodeSender(JavaMailSender mailSender, @Value("${app.mail.from}") String from,
                           @Value("${app.web-url}") String webUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.webUrl = webUrl;
    }

    public void send(EmailCodeRequestedEvent event) {
        EmailMessage message = message(event, webUrl);
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(event.email());
        mail.setSubject(message.subject());
        mail.setText(message.body());
        mailSender.send(mail);
        // сам код в лог не пишем
        log.info("Email code ({}) sent to user {}", event.purpose(), event.userId());
    }

    /**
     * Текст письма — отдельно от отправки, чтобы проверять тестом.
     * Три вида: код подтверждения, код смены пароля и приветствие от тренера с кодом подтверждения внутри
     * (клиент получил аккаунт — одно письмо вместо двух подряд, docs/trainer.md, шаг 5.3).
     */
    static EmailMessage message(EmailCodeRequestedEvent event, String webUrl) {
        String name = event.displayName() == null || event.displayName().isBlank() ? "Привет" : event.displayName();
        String validity = "Код действует 15 минут.";
        if (event.trainerName() != null) {
            String trainer = Boolean.TRUE.equals(event.keepTrainer())
                    ? "Тренер по-прежнему видит ваши тренировки и может записывать их за вас. Отключить тренера можно в профиле."
                    : "Дальше вы ведёте дневник сами: тренер больше не видит ваши тренировки.";
            // код и в теме: его видно прямо в уведомлении почты на телефоне
            return new EmailMessage("Vegas: тренер " + event.trainerName() + " передал вам дневник — код " + event.code(),
                    name + ",\n\n"
                            + "тренер " + event.trainerName() + " передал вам дневник тренировок: всё, что он записывал, "
                            + "теперь в вашем аккаунте — тренировки, рекорды и графики.\n"
                            + trainer + "\n\n"
                            + "Осталось подтвердить email — код: " + event.code() + "\n"
                            + validity + " Без подтверждения не получится восстановить пароль, если забудете.\n\n"
                            + "Открыть дневник: " + webUrl + "\n\n"
                            + "— Vegas, дневник тренировок");
        }
        boolean reset = "RESET_PASSWORD".equals(event.purpose());
        return new EmailMessage(
                reset ? "Vegas: код для смены пароля — " + event.code() : "Vegas: код подтверждения — " + event.code(),
                name + ",\n\n"
                        + (reset ? "ваш код для смены пароля: " : "ваш код подтверждения email: ") + event.code() + "\n\n"
                        + validity + " Если вы ничего не запрашивали — просто проигнорируйте это письмо.\n\n"
                        + "— Vegas, дневник тренировок");
    }
}
