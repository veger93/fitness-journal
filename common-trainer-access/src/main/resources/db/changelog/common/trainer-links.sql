--liquibase formatted sql

--changeset sergey:common-trainer-links
-- Копия связей "тренер — клиент" из user-service (события TrainerClientLinked в топике user-events).
-- Нужна, чтобы проверять доступ тренера к данным клиента прямо здесь, без запроса в user-service.
-- Файл общий (модуль common-trainer-access) — его подключают миграции каждого сервиса с данными клиента.
CREATE TABLE trainer_links (
    client_id  UUID        PRIMARY KEY,   -- у клиента не больше одного тренера
    trainer_id UUID        NOT NULL,
    linked_at  TIMESTAMPTZ NOT NULL
);
--rollback DROP TABLE trainer_links;
