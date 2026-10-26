package com.vegas.notification.service;

import com.vegas.common.event.MonthlyReportReadyEvent;
import com.vegas.common.event.OverloadRiskEvent;
import com.vegas.common.event.PersonalRecordEvent;
import com.vegas.common.event.PlateauDetectedEvent;
import com.vegas.notification.entity.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;

/**
 * Превращает события аналитики в письма. Главное здесь — ключи дублей:
 * один ключ = одно письмо, сколько бы раз событие ни пришло.
 */
@Service
public class AnalyticsNotifier {

    private final NotificationService notificationService;
    /** Адрес веб-приложения — для ссылок в письмах (локально http://localhost:5173) */
    private final String webUrl;

    /**
     * Конструктор вручную, а не @RequiredArgsConstructor: Lombok не переносит @Value с поля на параметр конструктора,
     * и Spring не знал бы, откуда взять строку.
     */
    public AnalyticsNotifier(NotificationService notificationService, @Value("${app.web-url}") String webUrl) {
        this.notificationService = notificationService;
        this.webUrl = webUrl;
    }

    /** Рекорд привязан к тренировке и упражнению. */
    public void personalRecord(PersonalRecordEvent event) {
        notificationService.send(event.userId(), NotificationType.PERSONAL_RECORD,
                "RECORD:" + event.workoutId() + ":" + event.exerciseId(),
                greeting -> NotificationTemplates.personalRecord(greeting, event));
    }

    /** Одно плато = одна дата последнего прогресса. Пока плато длится, дата не меняется — письмо одно. */
    public void plateau(PlateauDetectedEvent event) {
        notificationService.send(event.userId(), NotificationType.PLATEAU,
                "PLATEAU:" + event.exerciseId() + ":" + event.lastProgressAt(),
                greeting -> NotificationTemplates.plateau(greeting, event));
    }

    /** Риск — не чаще раза в неделю по упражнению (ISO-неделя, например 2026-W40). */
    public void overloadRisk(OverloadRiskEvent event) {
        notificationService.send(event.userId(), NotificationType.OVERLOAD_RISK,
                "RISK:" + event.exerciseId() + ":" + isoWeek(event),
                greeting -> NotificationTemplates.overloadRisk(greeting, event));
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
