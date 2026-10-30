package com.vegas.notification.service;

import com.vegas.common.event.MonthlyReportReadyEvent;
import com.vegas.common.event.OverloadRiskEvent;
import com.vegas.common.event.PersonalRecordEvent;
import com.vegas.common.event.PlateauDetectedEvent;
import com.vegas.notification.entity.EmailMessage;
import com.vegas.notification.entity.NotificationType;
import com.vegas.traineraccess.TrainerLinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.UUID;
import java.util.function.Function;

/**
 * Превращает события аналитики в письма. Главное здесь — ключи дублей:
 * один ключ = одно письмо, сколько бы раз событие ни пришло.
 */
@Service
public class AnalyticsNotifier {

    /** В старых событиях о связи нет имени клиента */
    static final String UNKNOWN_CLIENT = "Клиент";

    private final NotificationService notificationService;
    /** Копия связей "тренер — клиент" (из Kafka): есть у клиента тренер — уведомление дублируем ему */
    private final TrainerLinkRepository trainerLinks;
    /** Адрес веб-приложения — для ссылок в письмах (локально http://localhost:5173) */
    private final String webUrl;

    /**
     * Конструктор вручную, а не @RequiredArgsConstructor: Lombok не переносит @Value с поля на параметр конструктора,
     * и Spring не знал бы, откуда взять строку.
     */
    public AnalyticsNotifier(NotificationService notificationService, TrainerLinkRepository trainerLinks,
                             @Value("${app.web-url}") String webUrl) {
        this.notificationService = notificationService;
        this.trainerLinks = trainerLinks;
        this.webUrl = webUrl;
    }

    /** Рекорд привязан к тренировке и упражнению. */
    public void personalRecord(PersonalRecordEvent event) {
        notificationService.send(event.userId(), NotificationType.PERSONAL_RECORD,
                "RECORD:" + event.workoutId() + ":" + event.exerciseId(),
                greeting -> NotificationTemplates.personalRecord(greeting, event));
        toTrainer(event.userId(), NotificationType.PERSONAL_RECORD, "RECORD:" + event.workoutId() + ":" + event.exerciseId(),
                client -> NotificationTemplates.clientRecord(client, event));
    }

    /** Одно плато = одна дата последнего прогресса. Пока плато длится, дата не меняется — письмо одно. */
    public void plateau(PlateauDetectedEvent event) {
        notificationService.send(event.userId(), NotificationType.PLATEAU,
                "PLATEAU:" + event.exerciseId() + ":" + event.lastProgressAt(),
                greeting -> NotificationTemplates.plateau(greeting, event));
        toTrainer(event.userId(), NotificationType.PLATEAU, "PLATEAU:" + event.exerciseId() + ":" + event.lastProgressAt(),
                client -> NotificationTemplates.clientPlateau(client, event));
    }

    /** Риск — не чаще раза в неделю по упражнению (ISO-неделя, например 2026-W40). */
    public void overloadRisk(OverloadRiskEvent event) {
        notificationService.send(event.userId(), NotificationType.OVERLOAD_RISK,
                "RISK:" + event.exerciseId() + ":" + isoWeek(event),
                greeting -> NotificationTemplates.overloadRisk(greeting, event));
        toTrainer(event.userId(), NotificationType.OVERLOAD_RISK, "RISK:" + event.exerciseId() + ":" + isoWeek(event),
                client -> NotificationTemplates.clientOverload(client, event));
    }

    /**
     * Есть у клиента тренер — ему то же событие в ленту (docs/trainer.md, шаг 5.2).
     * Ключ с id клиента: у тренера свои рекорды и рекорды клиентов не должны "склеиться" по ключу.
     * Итоги месяца тренеру не шлём — это личное письмо клиенту.
     */
    private void toTrainer(UUID clientId, NotificationType type, String key, Function<String, EmailMessage> template) {
        trainerLinks.findById(clientId).ifPresent(link -> {
            String name = link.getClientName() != null ? link.getClientName() : UNKNOWN_CLIENT;
            notificationService.notifyTrainer(link.getTrainerId(), clientId, name, type,
                    "CLIENT:" + clientId + ":" + key, template.apply(name));
        });
    }

    /** Отчёт — один на месяц. */
    public void monthlyReport(MonthlyReportReadyEvent event) {
        notificationService.send(event.userId(), NotificationType.MONTHLY_REPORT,
                "REPORT:" + event.month(),
                greeting -> NotificationTemplates.monthlyReport(greeting, event, webUrl + "/reports/" + event.month()));
    }

    static String isoWeek(OverloadRiskEvent event) {
        ZonedDateTime time = event.detectedAt().atZone(ZoneOffset.UTC);
        return "%d-W%02d".formatted(time.get(IsoFields.WEEK_BASED_YEAR), time.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }
}
