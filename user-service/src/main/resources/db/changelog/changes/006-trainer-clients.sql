--liquibase formatted sql

--changeset sergey:006-trainer-clients
-- Аккаунты тренеров (docs/trainer.md).

-- "Я тренер" — переключатель в профиле: тренер остаётся обычным пользователем и тренируется сам
ALTER TABLE users ADD COLUMN trainer BOOLEAN NOT NULL DEFAULT FALSE;

-- Ведомый клиент: пользователь, которого завёл тренер. Входа нет (NONE): ни email, ни пароля —
-- пока тренер не передаст ему аккаунт. Все его данные с первого дня лежат под его собственным id.
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE users DROP CONSTRAINT ck_users_auth_provider;
ALTER TABLE users ADD CONSTRAINT ck_users_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'NONE'));
-- войти можно только по email: у всех, кроме ведомых, он обязателен
ALTER TABLE users ADD CONSTRAINT ck_users_email_required CHECK (auth_provider = 'NONE' OR email IS NOT NULL);
ALTER TABLE users ADD CONSTRAINT ck_users_managed_no_password CHECK (auth_provider <> 'NONE' OR password_hash IS NULL);

-- Связь "тренер — клиент". Закончилась (клиент ушёл) — ended_at заполнен, строку не удаляем: это история.
CREATE TABLE trainer_clients (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id  UUID        NOT NULL,
    client_id   UUID        NOT NULL,
    phone       VARCHAR(30),                  -- записная книжка тренера: уйдёт клиент — уйдёт и телефон
    linked_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at    TIMESTAMPTZ,

    CONSTRAINT fk_trainer_clients_trainer FOREIGN KEY (trainer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_trainer_clients_client  FOREIGN KEY (client_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_trainer_clients_not_self CHECK (trainer_id <> client_id)
);

-- У клиента не больше одного тренера одновременно. Частичный индекс: уникальность только среди активных связей,
-- закончившихся может быть сколько угодно.
CREATE UNIQUE INDEX uq_trainer_clients_active_client ON trainer_clients (client_id) WHERE ended_at IS NULL;
-- список "мои клиенты" — самый частый запрос тренера
CREATE INDEX ix_trainer_clients_active_trainer ON trainer_clients (trainer_id) WHERE ended_at IS NULL;

--rollback DROP TABLE trainer_clients;
--rollback ALTER TABLE users DROP CONSTRAINT ck_users_managed_no_password;
--rollback ALTER TABLE users DROP CONSTRAINT ck_users_email_required;
--rollback ALTER TABLE users DROP CONSTRAINT ck_users_auth_provider;
--rollback ALTER TABLE users ADD CONSTRAINT ck_users_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE'));
--rollback ALTER TABLE users ALTER COLUMN email SET NOT NULL;
--rollback ALTER TABLE users DROP COLUMN trainer;
