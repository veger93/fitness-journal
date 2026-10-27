--liquibase formatted sql

--changeset sergey:006-weekly-plan-reminders
-- Копия плана на неделю из workout-service (приходит событием WeeklyPlanUpdated через Kafka).
-- Своя копия, а не HTTP-запрос в workout-service каждую минуту: планировщик работает, даже если workout-service лежит.
CREATE TABLE weekly_plans (
    user_id     UUID        PRIMARY KEY,
    -- время изменения плана в workout-service: событие старее сохранённого — пропускаем
    updated_at  TIMESTAMPTZ NOT NULL
);

-- дни с тренировкой (дни отдыха не храним)
CREATE TABLE weekly_plan_days (
    user_id        UUID         NOT NULL REFERENCES weekly_plans (user_id) ON DELETE CASCADE,
    day_of_week    VARCHAR(9)   NOT NULL,
    template_name  VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, day_of_week)
);

-- PLAN — напоминать в дни плана (по умолчанию для новых настроек), CUSTOM — в выбранные вручную дни.
-- Уже сохранённые настройки остаются CUSTOM: у этих пользователей дни выбраны руками, не меняем их поведение молча.
ALTER TABLE notification_settings ADD COLUMN reminder_mode VARCHAR(10) NOT NULL DEFAULT 'CUSTOM';
ALTER TABLE notification_settings ADD CONSTRAINT ck_notification_settings_mode CHECK (reminder_mode IN ('PLAN', 'CUSTOM'));

-- в режиме PLAN дни берутся из плана, поэтому reminder_days может быть пустым
ALTER TABLE notification_settings DROP CONSTRAINT ck_notification_settings_reminders;
ALTER TABLE notification_settings ADD CONSTRAINT ck_notification_settings_reminders
    CHECK (NOT reminders_enabled OR (reminder_time IS NOT NULL AND (reminder_mode = 'PLAN' OR reminder_days IS NOT NULL)));
--rollback ALTER TABLE notification_settings DROP CONSTRAINT ck_notification_settings_reminders;
--rollback ALTER TABLE notification_settings ADD CONSTRAINT ck_notification_settings_reminders CHECK (NOT reminders_enabled OR (reminder_days IS NOT NULL AND reminder_time IS NOT NULL));
--rollback ALTER TABLE notification_settings DROP CONSTRAINT ck_notification_settings_mode;
--rollback ALTER TABLE notification_settings DROP COLUMN reminder_mode;
--rollback DROP TABLE weekly_plan_days;
--rollback DROP TABLE weekly_plans;
