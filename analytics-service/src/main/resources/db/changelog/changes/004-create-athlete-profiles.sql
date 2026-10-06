--liquibase formatted sql

--changeset sergey:004-create-athlete-profiles
-- Копия физ. данных пользователя из user-service (приходит событием UserProfileUpdated).
-- Нужна аналитике: уровень подготовки -> эталонная кривая; пол и вес — для будущих силовых стандартов.
CREATE TABLE athlete_profiles (
    user_id          UUID         PRIMARY KEY,
    gender           VARCHAR(10),
    birth_year       INTEGER,
    height_cm        INTEGER,
    experience_level VARCHAR(20),
    body_weight_kg   NUMERIC(5,2),
    -- момент изменения в user-service: более старое событие не перезапишет более новое
    updated_at       TIMESTAMPTZ  NOT NULL
);
--rollback DROP TABLE athlete_profiles;
