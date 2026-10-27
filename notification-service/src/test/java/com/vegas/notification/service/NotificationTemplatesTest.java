package com.vegas.notification.service;

import com.vegas.common.event.MonthlyReportReadyEvent;
import com.vegas.common.event.OverloadRiskEvent;
import com.vegas.common.event.PersonalRecordEvent;
import com.vegas.common.event.PlateauDetectedEvent;
import com.vegas.notification.entity.EmailMessage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTemplatesTest {

    @Test
    void personalRecord_listsRecordsWithFootnotes() {
        PersonalRecordEvent event = new PersonalRecordEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Жим лёжа", Instant.parse("2026-09-20T10:00:00Z"), new BigDecimal("102.50"), 5, List.of(
                new PersonalRecordEvent.Item("WEIGHT", new BigDecimal("102.50"), new BigDecimal("2.50"), "Самый большой вес"),
                new PersonalRecordEvent.Item("E1RM", new BigDecimal("119.58"), new BigDecimal("1.20"), "Тот же вес, но больше повторов")));

        EmailMessage message = NotificationTemplates.personalRecord("Сергей", event);

        assertThat(message.subject()).isEqualTo("Новый рекорд: Жим лёжа");
        assertThat(message.body())
                .startsWith("Сергей, новый рекорд!")
                .contains("Жим лёжа: 102.5 кг × 5")
                .contains("• Вес: 102.5 кг (+2.5 кг)*\n")
                .contains("• Сила: 119.58 кг (+1.2 кг)**\n")
                .contains("* Самый большой вес")
                .contains("** Тот же вес, но больше повторов");
    }

    @Test
    void personalRecord_firstRecordWithoutDelta() {
        PersonalRecordEvent event = new PersonalRecordEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Присед", Instant.now(), new BigDecimal("80"), 8, List.of(
                new PersonalRecordEvent.Item("WEIGHT", new BigDecimal("80"), null, null)));

        assertThat(NotificationTemplates.personalRecord("Привет", event).body())
                .contains("• Вес: 80 кг\n")
                .doesNotContain("*");
    }

    @Test
    void plateau_mentionsDaysAndStartDate() {
        PlateauDetectedEvent event = new PlateauDetectedEvent(UUID.randomUUID(), UUID.randomUUID(), "Присед",
                Instant.parse("2026-09-01T09:00:00Z"), 24);

        EmailMessage message = NotificationTemplates.plateau("Сергей", event);

        assertThat(message.subject()).isEqualTo("Плато: Присед");
        assertThat(message.body()).contains("уже 24 дн. (с 1 сентября)");
    }

    @Test
    void overloadRisk_listsReasons() {
        OverloadRiskEvent event = new OverloadRiskEvent(UUID.randomUUID(), UUID.randomUUID(), "Становая тяга", "HIGH",
                List.of("Нагрузка резко выросла", "Сила падает"), Instant.now());

        assertThat(NotificationTemplates.overloadRisk("Сергей", event).body())
                .contains("• Нагрузка резко выросла\n• Сила падает\n")
                .contains("не диагноз");
    }

    @Test
    void monthlyReport_showsNumbers() {
        MonthlyReportReadyEvent event = new MonthlyReportReadyEvent(UUID.randomUUID(), "2026-09",
                new BigDecimal("3.40"), new BigDecimal("45200.0"), new BigDecimal("-5.0"), 12, 4);

        EmailMessage message = NotificationTemplates.monthlyReport("Сергей", event,
                "http://localhost:5173/reports/2026-09");

        assertThat(message.subject()).isEqualTo("Итоги месяца: сентябрь 2026");
        assertThat(message.body())
                .contains("• Тренировок: 12")
                .contains("• Рекордов: 4")
                .contains("• Сила: +3.4%*")
                .contains("• Поднято за месяц: 45200 кг (-5%)**")
                .contains("Подробный отчёт: http://localhost:5173/reports/2026-09");
    }

    @Test
    void monthlyReport_withoutComparison_hasNoStrengthLine() {
        MonthlyReportReadyEvent event = new MonthlyReportReadyEvent(UUID.randomUUID(), "2026-01",
                null, null, null, 2, 0);

        assertThat(NotificationTemplates.monthlyReport("Привет", event, "http://x/reports/2026-01").body())
                .doesNotContain("Сила")
                .doesNotContain("*");
    }

    @Test
    void workoutReminder_fromPlan_namesTemplate() {
        EmailMessage message = NotificationTemplates.workoutReminder("Сергей", java.time.LocalDate.parse("2026-10-19"),
                java.util.Optional.of("Верх"));

        assertThat(message.subject()).isEqualTo("Сегодня по плану: Верх");
        assertThat(message.body()).startsWith("Сергей, сегодня (понедельник) по плану тренировка: Верх.");
    }

    @Test
    void workoutReminder_restDayInPlan_doesNotClaimPlan() {
        EmailMessage message = NotificationTemplates.workoutReminder("Сергей", java.time.LocalDate.parse("2026-10-20"),
                java.util.Optional.empty());

        assertThat(message.subject()).isEqualTo("Сегодня тренировка");
        assertThat(message.body()).startsWith("Сергей, напоминаем о тренировке сегодня (вторник).")
                .doesNotContain("по плану");
    }

    @Test
    void inactivity_showsDaysAndDate() {
        EmailMessage message = NotificationTemplates.inactivity("Сергей", 8, java.time.LocalDate.parse("2026-10-10"));

        assertThat(message.body())
                .contains("последняя тренировка была 8 дн. назад (10 октября)")
                .contains("* Пара недель перерыва");
    }
}
