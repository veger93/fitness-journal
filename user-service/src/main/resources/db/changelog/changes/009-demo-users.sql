--liquibase formatted sql

--changeset sergey:009-demo-users
-- Демо-аккаунты "посмотреть изнутри" (docs/demo.md): временный пользователь без email и пароля.
-- demo_expires_at — когда демо заканчивается (NULL у настоящих пользователей). Стоит и у клиентов демо-тренера:
-- по нему задача по расписанию находит, что удалить.
ALTER TABLE users ADD COLUMN demo_expires_at TIMESTAMPTZ;

ALTER TABLE users DROP CONSTRAINT ck_users_auth_provider;
ALTER TABLE users ADD CONSTRAINT ck_users_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'NONE', 'DEMO'));
-- email не нужен ни ведомому клиенту, ни демо
ALTER TABLE users DROP CONSTRAINT ck_users_email_required;
ALTER TABLE users ADD CONSTRAINT ck_users_email_required CHECK (auth_provider IN ('NONE', 'DEMO') OR email IS NOT NULL);
-- у демо пароля нет — как и у ведомого клиента
ALTER TABLE users ADD CONSTRAINT ck_users_demo_no_password CHECK (auth_provider <> 'DEMO' OR password_hash IS NULL);

-- задача удаления ищет истёкшие демо; частичный индекс — только по демо-строкам, настоящих в нём нет
CREATE INDEX idx_users_demo_expires ON users (demo_expires_at) WHERE demo_expires_at IS NOT NULL;
--rollback DROP INDEX idx_users_demo_expires; ALTER TABLE users DROP CONSTRAINT ck_users_demo_no_password; ALTER TABLE users DROP CONSTRAINT ck_users_email_required; ALTER TABLE users ADD CONSTRAINT ck_users_email_required CHECK (auth_provider = 'NONE' OR email IS NOT NULL); ALTER TABLE users DROP CONSTRAINT ck_users_auth_provider; ALTER TABLE users ADD CONSTRAINT ck_users_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'NONE')); ALTER TABLE users DROP COLUMN demo_expires_at;
