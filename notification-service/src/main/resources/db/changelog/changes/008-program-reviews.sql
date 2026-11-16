--liquibase formatted sql

--changeset sergey:008-program-reviews
-- Когда пересматривать программу тренировок (копия из workout-service, событие ProgramChanged через Kafka).
-- Одна строка на человека: у него одна активная программа. Отказался от программы — строку удаляем.
CREATE TABLE program_reviews (
    user_id     UUID        PRIMARY KEY,
    program_id  UUID        NOT NULL,
    -- день пересмотра (через 6 недель после старта); напоминание — в этот день с 9:00 по времени пользователя
    review_on   DATE        NOT NULL,
    -- время изменения программы в workout-service: событие старее сохранённого — пропускаем
    updated_at  TIMESTAMPTZ NOT NULL
);

-- планировщик ищет «созревшие»: review_on <= сегодня
CREATE INDEX idx_program_reviews_review_on ON program_reviews (review_on);
--rollback DROP TABLE program_reviews;
