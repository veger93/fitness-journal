package com.vegas.notification.service;

import com.vegas.notification.entity.EmailMessage;
import com.vegas.notification.entity.Notification;
import com.vegas.notification.entity.NotificationStatus;
import com.vegas.notification.entity.NotificationType;
import com.vegas.notification.entity.Recipient;
import com.vegas.notification.repository.NotificationRepository;
import com.vegas.notification.repository.RecipientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final UUID USER = UUID.randomUUID();
    private static final String KEY = "REPORT:2026-09";

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private RecipientRepository recipientRepository;
    @Mock
    private JavaMailSender mailSender;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, recipientRepository, mailSender,
                Clock.fixed(NOW, ZoneOffset.UTC), "Vegas <no-reply@vegas.local>");
    }

    @Test
    void sendsEmailAndSavesJournal() {
        when(recipientRepository.findById(USER)).thenReturn(Optional.of(new Recipient(USER, "s@mail.ru", "Сергей")));
        // настоящий save возвращает сохранённый объект — мок по умолчанию вернул бы null
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.send(USER, NotificationType.MONTHLY_REPORT, KEY, greeting -> new EmailMessage("Тема", greeting + ", текст"));

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly("s@mail.ru");
        assertThat(mail.getValue().getFrom()).isEqualTo("Vegas <no-reply@vegas.local>");
        assertThat(mail.getValue().getText()).isEqualTo("Сергей, текст");

        Notification saved = savedNotification();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.getDedupeKey()).isEqualTo(KEY);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void duplicate_isIgnored() {
        when(notificationRepository.existsByUserIdAndDedupeKey(USER, KEY)).thenReturn(true);

        service.send(USER, NotificationType.MONTHLY_REPORT, KEY, greeting -> new EmailMessage("Тема", "текст"));

        verifyNoInteractions(mailSender, recipientRepository);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void noRecipient_isSkippedWithoutEmail() {
        when(recipientRepository.findById(USER)).thenReturn(Optional.empty());
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Notification> result = service.send(USER, NotificationType.MONTHLY_REPORT, KEY,
                greeting -> new EmailMessage("Тема", greeting));

        assertThat(result).isPresent();

        verifyNoInteractions(mailSender);
        Notification saved = savedNotification();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        assertThat(saved.getReason()).isEqualTo(NotificationService.NO_RECIPIENT);
        assertThat(saved.getBody()).isEqualTo("Привет");
    }

    @Test
    void mailFailure_isRethrown_soKafkaRetries() {
        when(recipientRepository.findById(USER)).thenReturn(Optional.of(new Recipient(USER, "s@mail.ru", null)));
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> service.send(USER, NotificationType.MONTHLY_REPORT, KEY,
                greeting -> new EmailMessage("Тема", "текст")))
                .isInstanceOf(MailSendException.class);
        verify(notificationRepository, never()).save(any());
    }

    private Notification savedNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue();
    }
}
