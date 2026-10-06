--liquibase formatted sql

--changeset sergey:001-create-recipients
-- Кому слать письма. Заполняется из события UserRegistered (user-service).
CREATE TABLE recipients (
    user_id      UUID         PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
--rollback DROP TABLE recipients;
