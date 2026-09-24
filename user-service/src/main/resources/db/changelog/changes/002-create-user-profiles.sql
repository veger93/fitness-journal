--liquibase formatted sql

--changeset sergey:002-create-user-profiles
-- Физ. данные из онбординга. Связь 1:1 с users: первичный ключ = внешний ключ.
CREATE TABLE user_profiles (
    user_id              UUID        PRIMARY KEY,
    gender               VARCHAR(10),
    birth_year           SMALLINT,                        -- год, а не возраст: возраст устаревает
    height_cm            SMALLINT,
    experience_level     VARCHAR(20),
    onboarding_completed BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_profiles_user   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_profiles_gender CHECK (gender IN ('MALE', 'FEMALE')),
    CONSTRAINT ck_user_profiles_birth  CHECK (birth_year >= 1900),
    CONSTRAINT ck_user_profiles_height CHECK (height_cm BETWEEN 100 AND 250),
    CONSTRAINT ck_user_profiles_level  CHECK (experience_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);
--rollback DROP TABLE user_profiles;
