package com.vegas.notification.service;

import com.vegas.common.event.EmailCodeRequestedEvent;
import com.vegas.notification.entity.EmailMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailCodeSenderTest {

    private static final String WEB = "https://vegas-fit.ru";

    @Test
    void verification_plainCodeEmail() {
        EmailMessage message = EmailCodeSender.message(event("VERIFY_EMAIL", null, null), WEB);

        assertThat(message.subject()).isEqualTo("Vegas: код подтверждения — 123456");
        assertThat(message.body()).startsWith("Иван,\n\nваш код подтверждения email: 123456");
    }

    @Test
    void claimedFromTrainer_welcomeWithCode() {
        EmailMessage message = EmailCodeSender.message(event("VERIFY_EMAIL", "Сергей", true), WEB);

        assertThat(message.subject()).isEqualTo("Vegas: тренер Сергей передал вам дневник — код 123456");
        assertThat(message.body())
                .contains("тренер Сергей передал вам дневник тренировок")
                .contains("Тренер по-прежнему видит ваши тренировки")
                .contains("код: 123456")
                .contains("Открыть дневник: " + WEB);
    }

    @Test
    void claimedGoingAlone_saysTrainerNoLongerSees() {
        EmailMessage message = EmailCodeSender.message(event("VERIFY_EMAIL", "Сергей", false), WEB);

        assertThat(message.body()).contains("тренер больше не видит ваши тренировки");
    }

    private static EmailCodeRequestedEvent event(String purpose, String trainer, Boolean keepTrainer) {
        return new EmailCodeRequestedEvent(UUID.randomUUID(), "ivan@mail.ru", "Иван", "123456", purpose,
                Instant.now(), trainer, keepTrainer);
    }
}
