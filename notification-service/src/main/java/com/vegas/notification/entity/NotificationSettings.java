package com.vegas.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** Необязательные уведомления пользователя. Аналитические включены всегда и здесь не хранятся. */
@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSettings {

    public static final int DEFAULT_INACTIVITY_DAYS = 7;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "reminders_enabled", nullable = false)
    private boolean remindersEnabled;

    @Convert(converter = DaysOfWeekConverter.class)
    @Column(name = "reminder_days", length = 70)
    private Set<DayOfWeek> reminderDays = EnumSet.noneOf(DayOfWeek.class);

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "inactivity_enabled", nullable = false)
    private boolean inactivityEnabled;

    @Column(name = "inactivity_days", nullable = false)
    private int inactivityDays = DEFAULT_INACTIVITY_DAYS;

    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone = "UTC";

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Настройки по умолчанию: всё необязательное выключено. */
    public static NotificationSettings defaults(UUID userId) {
        NotificationSettings settings = new NotificationSettings();
        settings.userId = userId;
        return settings;
    }

    public void update(boolean remindersEnabled, Set<DayOfWeek> reminderDays, LocalTime reminderTime,
                       boolean inactivityEnabled, int inactivityDays, String timeZone) {
        this.remindersEnabled = remindersEnabled;
        // EnumSet.copyOf(Collection) падает на пустой коллекции — поэтому сначала пустой set, потом addAll
        this.reminderDays = EnumSet.noneOf(DayOfWeek.class);
        if (reminderDays != null) {
            this.reminderDays.addAll(reminderDays);
        }
        this.reminderTime = reminderTime;
        this.inactivityEnabled = inactivityEnabled;
        this.inactivityDays = inactivityDays;
        this.timeZone = timeZone;
    }
}
