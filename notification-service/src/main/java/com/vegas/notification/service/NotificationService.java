package com.vegas.notification.service;

import com.vegas.notification.entity.EmailMessage;
import com.vegas.notification.entity.Notification;
import com.vegas.notification.entity.NotificationType;
import com.vegas.notification.entity.Recipient;
import com.vegas.notification.repository.NotificationRepository;
import com.vegas.notification.repository.RecipientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Единая точка отправки: проверка дублей -> письмо -> запись в журнал.
 *
 * Зачем dedupeKey. Kafka гарантирует "хотя бы один раз" — одно и то же событие может прийти дважды,
 * а плато/риск аналитика присылает после каждой тренировки. Ключ описывает СМЫСЛ уведомления
 * ("плато в приседе с 1 сентября"), и второе такое письмо не уходит.
 * Последняя линия защиты — UNIQUE(user_id, dedupe_key) в БД.
 *
 * Если письмо не ушло (почтовый сервер недоступен) — бросаем исключение: транзакция откатывается,
 * Kafka повторит сообщение (KafkaConsumerConfig), а после 3 неудач оно уйдёт в DLT.
 */
@Slf4j
@Service
public class NotificationService {

    static final String NO_RECIPIENT = "Нет email получателя (событие UserRegistered ещё не пришло)";

    private final NotificationRepository notificationRepository;
    private final RecipientRepository recipientRepository;
    private final JavaMailSender mailSender;
    private final Clock clock;
    private final String from;

    public NotificationService(NotificationRepository notificationRepository,
                               RecipientRepository recipientRepository,
                               JavaMailSender mailSender,
                               Clock clock,
                               @Value("${app.mail.from}") String from) {
        this.notificationRepository = notificationRepository;
        this.recipientRepository = recipientRepository;
        this.mailSender = mailSender;
        this.clock = clock;
        this.from = from;
    }

    /**
     * @param template собирает письмо; получает обращение к пользователю (имя или "Привет")
     * @return запись журнала (SENT или SKIPPED); empty — такое уведомление уже было (дубль по ключу)
     */
    @Transactional
    public Optional<Notification> send(UUID userId, NotificationType type, String dedupeKey,
                                       Function<String, EmailMessage> template) {
        if (notificationRepository.existsByUserIdAndDedupeKey(userId, dedupeKey)) {
            log.debug("Notification {} for user {} already handled", dedupeKey, userId);
            return Optional.empty();
        }

        Optional<Recipient> recipient = recipientRepository.findById(userId);
        EmailMessage message = template.apply(recipient.map(Recipient::greetingName).orElse("Привет"));

        if (recipient.isEmpty()) {
            // записываем как SKIPPED: видно в журнале, и при повторе события не будем пытаться снова
            log.warn("No recipient for user {}, notification {} skipped", userId, dedupeKey);
            return Optional.of(notificationRepository.save(
                    Notification.skipped(userId, type, dedupeKey, message, NO_RECIPIENT, clock.instant())));
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(recipient.get().getEmail());
        mail.setSubject(message.subject());
        mail.setText(message.body());
        mailSender.send(mail);

        Notification saved = notificationRepository.save(Notification.sent(userId, type, dedupeKey, message, clock.instant()));
        log.info("Notification {} sent to user {}", dedupeKey, userId);
        return Optional.of(saved);
    }

    /**
     * Тренеру — о событии у его клиента. Только в ленту приложения: клиентов десятки, письмо на каждое — спам.
     * Повтор события (Kafka доставляет "минимум один раз") отсекается тем же ключом, что и у обычных уведомлений.
     */
    @Transactional
    public Optional<Notification> notifyTrainer(UUID trainerId, UUID clientId, String clientName, NotificationType type,
                                                String dedupeKey, EmailMessage message) {
        if (notificationRepository.existsByUserIdAndDedupeKey(trainerId, dedupeKey)) {
            return Optional.empty();
        }
        Notification saved = notificationRepository.save(
                Notification.aboutClient(trainerId, type, dedupeKey, message, clientId, clientName, clock.instant()));
        log.info("Notification {} about client {} for trainer {}", dedupeKey, clientId, trainerId);
        return Optional.of(saved);
    }
}
