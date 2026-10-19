-- notification_db: очистка журнала писем, получателей и дат тренировок.
-- Получатель появится снова из события UserRegistered (01-user.sql -> outbox -> Kafka).
-- Настройки уведомлений вашего пользователя сохраняем, чужие удаляем.

\set ON_ERROR_STOP on
BEGIN;
TRUNCATE notifications;
TRUNCATE recipients;
-- даты тренировок заполнятся заново из WorkoutCompleted (02-workouts.sql -> outbox -> Kafka)
TRUNCATE workout_activity;
DELETE FROM notification_settings WHERE user_id <> :'user_id'::uuid;
COMMIT;
\echo 'notification_db: журнал и получатели очищены'
