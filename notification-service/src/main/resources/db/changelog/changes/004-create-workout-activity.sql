--liquibase formatted sql

--changeset sergey:004-create-workout-activity
-- Когда пользователь тренировался. Заполняется из workout-events (WorkoutCompleted / WorkoutDeleted).
-- Храним каждую тренировку, а не одну "дату последней": если удалить последнюю тренировку,
-- последней станет предыдущая — max(completed_at) пересчитается сам.
-- workout_id — PRIMARY KEY: повторное событие из Kafka не создаст дубль.
CREATE TABLE workout_activity (
    workout_id   UUID        PRIMARY KEY,
    user_id      UUID        NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_workout_activity_user_completed ON workout_activity (user_id, completed_at DESC);
--rollback DROP TABLE workout_activity;
