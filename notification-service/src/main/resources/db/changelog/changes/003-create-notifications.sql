--liquibase formatted sql

--changeset sergey:003-create-notifications
-- Журнал уведомлений: история для пользователя + защита от дублей.
-- dedupe_key — "отпечаток" события: одно плато (с одной даты) = одно письмо,
-- даже если аналитика сообщает о нём после каждой тренировки или Kafka доставила событие дважды.
CREATE TABLE notifications (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL,
    type        VARCHAR(30)   NOT NULL,
    dedupe_key  VARCHAR(200)  NOT NULL,
    subject     VARCHAR(200)  NOT NULL,
    body        TEXT          NOT NULL,
    status      VARCHAR(20)   NOT NULL,                 -- SENT / SKIPPED
    reason      VARCHAR(500),                           -- почему SKIPPED
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_notifications_dedupe UNIQUE (user_id, dedupe_key),
    CONSTRAINT ck_notifications_status CHECK (status IN ('SENT', 'SKIPPED'))
);
-- история пользователя, новые сверху (UNIQUE выше начинается с user_id, но сортировки по дате не даёт)
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
--rollback DROP TABLE notifications;
