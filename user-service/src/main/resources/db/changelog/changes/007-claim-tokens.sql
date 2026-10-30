--liquibase formatted sql

--changeset sergey:007-claim-tokens
-- Передача аккаунта клиенту (docs/trainer.md, шаг 4): тренер создаёт одноразовую ссылку/QR,
-- клиент по ней придумывает email и пароль для своего (уже существующего) аккаунта.
CREATE TABLE claim_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID        NOT NULL,
    trainer_id  UUID        NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,     -- SHA-256 в hex: сам токен не храним, как и refresh-токены
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_claim_tokens_hash    UNIQUE (token_hash),
    CONSTRAINT fk_claim_tokens_client  FOREIGN KEY (client_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_claim_tokens_trainer FOREIGN KEY (trainer_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX ix_claim_tokens_client ON claim_tokens (client_id);

-- Когда пользователь согласился на обработку своих данных (клиент — при получении аккаунта от тренера)
ALTER TABLE users ADD COLUMN data_consent_at TIMESTAMPTZ;

--rollback ALTER TABLE users DROP COLUMN data_consent_at;
--rollback DROP TABLE claim_tokens;
