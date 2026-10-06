--liquibase formatted sql

--changeset sergey:002-create-notification-settings
-- Настройки уведомлений (экран "Профиль -> уведомления").
-- Аналитические уведомления (плато, перегрузка, рекорд, отчёт) включены всегда и здесь не хранятся.
-- Необязательные — выключены по умолчанию. Строка появляется при первом сохранении настроек.
CREATE TABLE notification_settings (
    user_id            UUID        PRIMARY KEY,
    reminders_enabled  BOOLEAN     NOT NULL DEFAULT FALSE,
    reminder_days      VARCHAR(70),                        -- "MONDAY,WEDNESDAY,FRIDAY"
    reminder_time      TIME,                                -- местное время пользователя
    inactivity_enabled BOOLEAN     NOT NULL DEFAULT FALSE,
    inactivity_days    INTEGER     NOT NULL DEFAULT 7,
    time_zone          VARCHAR(50) NOT NULL DEFAULT 'UTC',  -- "Asia/Yekaterinburg"
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_notification_settings_inactivity CHECK (inactivity_days BETWEEN 3 AND 30),
    -- включённые напоминания без дней или времени не имеют смысла
    CONSTRAINT ck_notification_settings_reminders  CHECK (NOT reminders_enabled OR (reminder_days IS NOT NULL AND reminder_time IS NOT NULL))
);
--rollback DROP TABLE notification_settings;
