--liquibase formatted sql

--changeset sergey:003-create-exercise-media
-- Медиа к упражнению (картинка, Lottie-анимация, видео). Пока пустая: закладываем модель под анимации.
-- Сами файлы лежат в S3/MinIO, здесь только ключ файла (не URL: адрес хранилища может поменяться).
CREATE TABLE exercise_media (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    exercise_id UUID         NOT NULL,
    gender      VARCHAR(10),                              -- NULL = подходит для обоих полов
    media_type  VARCHAR(20)  NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_exercise_media_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT ck_exercise_media_gender   CHECK (gender IN ('MALE', 'FEMALE')),
    CONSTRAINT ck_exercise_media_type     CHECK (media_type IN ('IMAGE', 'LOTTIE', 'VIDEO'))
);

-- у упражнения один файл каждого типа на пол (и один "общий")
CREATE UNIQUE INDEX uq_exercise_media ON exercise_media (exercise_id, media_type, gender) NULLS NOT DISTINCT;
--rollback DROP TABLE exercise_media;
