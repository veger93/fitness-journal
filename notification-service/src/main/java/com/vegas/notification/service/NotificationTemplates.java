package com.vegas.notification.service;

import com.vegas.common.event.MonthlyReportReadyEvent;
import com.vegas.common.event.OverloadRiskEvent;
import com.vegas.common.event.PersonalRecordEvent;
import com.vegas.common.event.PlateauDetectedEvent;
import com.vegas.notification.entity.EmailMessage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Тексты писем. Простой текст без HTML: его проще тестировать и он одинаково выглядит в любом почтовике.
 *
 * Принцип из обсуждения с пользователем: спортсмену не нужны формулы.
 * В основном тексте — простые слова, пояснения — мелкими сносками (*) внизу.
 *
 * Класс без зависимостей (только статические методы) — тестируется без Spring и моков.
 */
public final class NotificationTemplates {

    private static final Locale RU = Locale.forLanguageTag("ru");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM", RU);
    private static final String SIGNATURE = "\n\n—\nVegas, дневник тренировок";

    private NotificationTemplates() {
    }

    public static EmailMessage personalRecord(String greeting, PersonalRecordEvent event) {
        StringBuilder body = new StringBuilder()
                .append(greeting).append(", новый рекорд!\n\n")
                .append(event.exerciseName());
        if (event.weightKg() != null) {
            body.append(": ").append(kg(event.weightKg()));
        }
        if (event.weightKg() != null && event.reps() != null) {
            body.append(" × ").append(event.reps());
        }
        body.append("\n\n");

        List<String> footnotes = new ArrayList<>();
        for (PersonalRecordEvent.Item item : event.records()) {
            body.append("• ").append(recordLabel(item.type())).append(": ").append(kg(item.valueKg()));
            if (item.deltaKg() != null && item.deltaKg().signum() > 0) {
                body.append(" (").append(signed(item.deltaKg())).append(" кг)");
            }
            if (item.hint() != null && !item.hint().isBlank()) {
                footnotes.add(item.hint());
                body.append("*".repeat(footnotes.size()));
            }
            body.append('\n');
        }
        appendFootnotes(body, footnotes);

        return new EmailMessage("Новый рекорд: " + event.exerciseName(), body + SIGNATURE);
    }

    public static EmailMessage plateau(String greeting, PlateauDetectedEvent event) {
        String body = greeting + ", в упражнении «" + event.exerciseName() + "» нет нового максимума уже "
                + event.days() + " дн. (с " + date(event) + ").*\n\n"
                + "Это нормальная часть тренировок, такое бывает у всех. Что можно попробовать:\n"
                + "• поменять число повторений (например, 5 вместо 8–10);\n"
                + "• добавить один подход;\n"
                + "• взять лёгкую неделю и вернуться отдохнувшим.\n"
                + "\n* Плато — 3 недели и больше без рекорда, хотя тренировки были.";
        return new EmailMessage("Плато: " + event.exerciseName(), body + SIGNATURE);
    }

    public static EmailMessage overloadRisk(String greeting, OverloadRiskEvent event) {
        StringBuilder body = new StringBuilder()
                .append(greeting).append(", похоже, в упражнении «").append(event.exerciseName())
                .append("» накопилась усталость.*\n\nНа что мы обратили внимание:\n");
        for (String reason : event.reasons()) {
            body.append("• ").append(reason).append('\n');
        }
        body.append("\nПопробуйте неделю полегче: план разгрузки есть в приложении, в разделе упражнения.\n")
                .append("\n* Это подсказка по вашим записям, а не диагноз. Если что-то болит — лучше обратиться к врачу.");
        return new EmailMessage("Пора немного отдохнуть: " + event.exerciseName(), body + SIGNATURE);
    }

    /** reportUrl — ссылка на страницу отчёта в приложении: /reports/2026-09 */
    public static EmailMessage monthlyReport(String greeting, MonthlyReportReadyEvent event, String reportUrl) {
        String month = monthName(event.month());
        StringBuilder body = new StringBuilder()
                .append(greeting).append(", итоги за ").append(month).append(":\n\n")
                .append("• Тренировок: ").append(event.workouts()).append('\n')
                .append("• Рекордов: ").append(event.records()).append('\n');
        List<String> footnotes = new ArrayList<>();
        if (event.strengthChangePercent() != null) {
            footnotes.add("Сила — насколько выросли ваши лучшие результаты в упражнениях по сравнению с прошлым месяцем.");
            body.append("• Сила: ").append(signed(event.strengthChangePercent())).append("%*\n");
        }
        if (event.tonnageKg() != null) {
            body.append("• Поднято за месяц: ").append(kg(event.tonnageKg()));
            if (event.tonnageChangePercent() != null) {
                body.append(" (").append(signed(event.tonnageChangePercent())).append("%)");
            }
            footnotes.add("Поднято за месяц — сумма «вес × повторы» по всем рабочим подходам.");
            body.append("*".repeat(footnotes.size())).append('\n');
        }
        body.append("\nПодробный отчёт: ").append(reportUrl).append('\n');
        appendFootnotes(body, footnotes);
        return new EmailMessage("Итоги месяца: " + month, body + SIGNATURE);
    }

    public static EmailMessage workoutReminder(String greeting, LocalDate day) {
        String weekday = day.getDayOfWeek().getDisplayName(TextStyle.FULL, RU);
        String body = greeting + ", сегодня (" + weekday + ") по плану тренировка. Хорошей работы!\n\n"
                + "Дни и время напоминаний можно поменять или выключить в профиле.";
        return new EmailMessage("Сегодня тренировка", body + SIGNATURE);
    }

    public static EmailMessage inactivity(String greeting, long days, LocalDate lastWorkoutDay) {
        String body = greeting + ", последняя тренировка была " + days + " дн. назад (" + DATE.format(lastWorkoutDay) + ").\n\n"
                + "Даже короткая тренировка помогает не терять форму. Не нужно наверстывать всё сразу — "
                + "начните с привычных упражнений и весов чуть легче обычного.*\n\n"
                + "* Пара недель перерыва на силе почти не сказывается, а то, что ушло за долгий перерыв, "
                + "возвращается быстрее, чем набиралось в первый раз.\n\n"
                + "Частоту этих писем можно настроить в профиле.";
        return new EmailMessage("Давно не было тренировок", body + SIGNATURE);
    }

    private static void appendFootnotes(StringBuilder body, List<String> footnotes) {
        if (footnotes.isEmpty()) {
            return;
        }
        body.append('\n');
        for (int i = 0; i < footnotes.size(); i++) {
            body.append("*".repeat(i + 1)).append(' ').append(footnotes.get(i)).append('\n');
        }
    }

    private static String recordLabel(String type) {
        return switch (type) {
            case "WEIGHT" -> "Вес";
            case "E1RM" -> "Сила";
            default -> type;
        };
    }

    private static String date(PlateauDetectedEvent event) {
        LocalDate day = event.lastProgressAt().atZone(ZoneOffset.UTC).toLocalDate();
        return DATE.format(day);
    }

    /** "2026-09" -> "сентябрь 2026". */
    static String monthName(String month) {
        YearMonth ym = YearMonth.parse(month);
        return ym.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, RU) + " " + ym.getYear();
    }

    /** 100.00 -> "100 кг", 102.50 -> "102.5 кг". */
    static String kg(BigDecimal value) {
        return number(value) + " кг";
    }

    static String signed(BigDecimal value) {
        return (value.signum() > 0 ? "+" : "") + number(value);
    }

    private static String number(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
