--liquibase formatted sql

--changeset sergey:003-create-body-weight-log
-- История веса тела. Текущий вес = последняя запись.
CREATE TABLE body_weight_log (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL,
    weight_kg   NUMERIC(5, 2) NOT NULL,                   -- до 999.99, ровно 2 знака: без ошибок округления double
    measured_on DATE          NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_body_weight_user      FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- одна запись в день; этот же уникальный индекс ускоряет выборку истории пользователя
    CONSTRAINT uq_body_weight_user_date UNIQUE (user_id, measured_on),
    CONSTRAINT ck_body_weight_range     CHECK (weight_kg > 20 AND weight_kg < 400)
);
--rollback DROP TABLE body_weight_log;
