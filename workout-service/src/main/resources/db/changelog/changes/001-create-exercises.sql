--liquibase formatted sql

--changeset sergey:001-create-exercises
-- Каталог упражнений: системные (owner_id IS NULL) + свои упражнения пользователей.
CREATE TABLE exercises (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id      UUID,                                   -- id пользователя из user-service. FK нет: это другая БД
    name          VARCHAR(100)  NOT NULL,
    body_part     VARCHAR(20)   NOT NULL,
    equipment     VARCHAR(20)   NOT NULL,
    tracking_type VARCHAR(20)   NOT NULL,
    icon          VARCHAR(30),                            -- ключ иконки из набора на фронте
    note          VARCHAR(1000),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT ck_exercises_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_exercises_body_part      CHECK (body_part IN ('CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'ARMS', 'CORE')),
    CONSTRAINT ck_exercises_equipment      CHECK (equipment IN ('BARBELL', 'DUMBBELL', 'MACHINE', 'BODYWEIGHT')),
    CONSTRAINT ck_exercises_tracking_type  CHECK (tracking_type IN ('WEIGHT_REPS', 'TIME', 'DISTANCE'))
);

-- Одно название на владельца, без учёта регистра ("Жим" = "жим").
-- NULLS NOT DISTINCT (PostgreSQL 15+): по умолчанию NULL != NULL, и у системных упражнений
-- (owner_id = NULL) дубли названий прошли бы. С этой опцией NULL-ы считаются равными.
-- Этот же индекс ускоряет выборку "всё по owner_id".
CREATE UNIQUE INDEX uq_exercises_owner_name ON exercises (owner_id, lower(name)) NULLS NOT DISTINCT;
--rollback DROP TABLE exercises;
