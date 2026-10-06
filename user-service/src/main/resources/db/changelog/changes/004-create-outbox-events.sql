--liquibase formatted sql

--changeset sergey:004-create-outbox-events
-- Transactional Outbox: событие сначала пишется в ЭТУ таблицу в той же транзакции,
-- что и бизнес-изменение (сохранение профиля, запись веса). Отдельный фоновый процесс
-- (OutboxRelay) читает неотправленные события и публикует их в Kafka.
-- Итог: событие не потеряется, даже если Kafka в этот момент лежала.
CREATE TABLE outbox_events (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID          NOT NULL,                  -- id пользователя
    event_type   VARCHAR(50)   NOT NULL,                  -- UserProfileUpdated
    event_key    VARCHAR(100)  NOT NULL,                  -- ключ Kafka-сообщения (userId)
    payload      TEXT          NOT NULL,                  -- JSON события
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,                             -- NULL = ещё не отправлено
    attempts     INTEGER       NOT NULL DEFAULT 0,
    last_error   VARCHAR(1000)
);

-- частичный индекс только по неотправленным: их мало, индекс маленький и быстрый
CREATE INDEX idx_outbox_events_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
--rollback DROP TABLE outbox_events;
