--liquibase formatted sql

--changeset sergey:005-notifications-read-at
-- Лента уведомлений в приложении: когда пользователь их увидел.
-- NULL — ещё не прочитано (у старых записей тоже NULL: они станут прочитанными при первом открытии ленты).
ALTER TABLE notifications ADD COLUMN read_at TIMESTAMPTZ;

-- счётчик на колокольчике: "сколько у пользователя непрочитанных".
-- Частичный индекс хранит только строки с read_at IS NULL — он маленький, и прочитанные его не раздувают.
CREATE INDEX idx_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;
--rollback DROP INDEX idx_notifications_user_unread; ALTER TABLE notifications DROP COLUMN read_at;
