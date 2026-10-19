-- notification_db: очистка журнала писем и получателей.
-- Получатель появится снова из события UserRegistered (01-user.sql -> outbox -> Kafka).
-- Настройки уведомлений вашего пользователя сохраняем, чужие удаляем.

\set ON_ERROR_STOP on
BEGIN;
TRUNCATE notifications;
TRUNCATE recipients;
DELETE FROM notification_settings WHERE user_id <> :'user_id'::uuid;
COMMIT;
\echo 'notification_db: журнал и получатели очищены'
