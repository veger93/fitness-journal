package com.vegas.notification.entity;

public enum NotificationType {
    PERSONAL_RECORD,
    PLATEAU,
    OVERLOAD_RISK,
    MONTHLY_REPORT,
    WORKOUT_REMINDER,  // напоминание по расписанию
    INACTIVITY,        // давно не было тренировок
    PROGRAM_REVIEW     // пора пересмотреть программу тренировок (через 6 недель)
}
