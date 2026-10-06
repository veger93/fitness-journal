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
    public static final String USER_PROFILE_UPDATED = "UserProfileUpdated";

    private EventTypes() {
    }
}
