package com.vegas.common.event;

/**
 * Имена типов событий. Передаются в Kafka в заголовке eventType,
 * по нему получатель понимает, в какой класс разобрать JSON.
 * Строка, а не имя Java-класса: класс можно переименовать или перенести, и контракт не сломается.
 */
public final class EventTypes {

    /** Имя заголовка Kafka-сообщения с типом события. */
    public static final String HEADER = "eventType";

    public static final String WORKOUT_COMPLETED = "WorkoutCompleted";
    public static final String WORKOUT_DELETED = "WorkoutDeleted";
    /** workout-service -> notification-service: напоминания "как в плане на неделю" */
    public static final String WEEKLY_PLAN_UPDATED = "WeeklyPlanUpdated";
    public static final String USER_PROFILE_UPDATED = "UserProfileUpdated";
    public static final String USER_REGISTERED = "UserRegistered";
    /** user-service -> workout/analytics: связь "тренер — клиент" (копия для проверки доступа) */
    public static final String TRAINER_CLIENT_LINKED = "TrainerClientLinked";
    /** связь закончилась (клиент ушёл от тренера) — копии в сервисах удаляют её */
    public static final String TRAINER_CLIENT_UNLINKED = "TrainerClientUnlinked";
    /** пользователь удалён — все сервисы удаляют его данные */
    public static final String USER_DELETED = "UserDeleted";

    // события analytics-service (топик analytics-events) -> notification-service
    public static final String PERSONAL_RECORD = "PersonalRecord";
    public static final String PLATEAU_DETECTED = "PlateauDetected";
    public static final String OVERLOAD_RISK = "OverloadRisk";
    public static final String MONTHLY_REPORT_READY = "MonthlyReportReady";

    private EventTypes() {
    }
}
