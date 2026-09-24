--liquibase formatted sql

--changeset sergey:001-create-users
-- Учётная запись: только то, что нужно для входа в систему.
CREATE TABLE users (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(100),                          -- NULL у тех, кто вошёл через Google
    auth_provider  VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    provider_id    VARCHAR(255),                          -- id пользователя у Google
    display_name   VARCHAR(100),
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email             UNIQUE (email),
    CONSTRAINT uq_users_provider          UNIQUE (auth_provider, provider_id),
    CONSTRAINT ck_users_auth_provider     CHECK (auth_provider IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT ck_users_role              CHECK (role IN ('USER', 'ADMIN')),
    -- у обычной регистрации пароль обязателен
    CONSTRAINT ck_users_local_has_password CHECK (auth_provider <> 'LOCAL' OR password_hash IS NOT NULL)
);
--rollback DROP TABLE users;
