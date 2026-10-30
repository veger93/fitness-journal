--liquibase formatted sql

--changeset sergey:008-email-codes
-- Подтверждение email и восстановление пароля кодом из 6 цифр (docs/account.md).

-- Когда пользователь подтвердил email. NULL — не подтверждён (пользоваться можно, но "Забыли пароль?" не работает)
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;

CREATE TABLE email_codes (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    purpose     VARCHAR(20)  NOT NULL,     -- VERIFY_EMAIL / RESET_PASSWORD
    email       VARCHAR(255) NOT NULL,     -- куда отправили: сменил email — старый код не подтвердит новый адрес
    code_hash   VARCHAR(64)  NOT NULL,     -- SHA-256 от "userId:код" — сам код не храним
    attempts    SMALLINT     NOT NULL DEFAULT 0,   -- неверных попыток: после 5 код сгорает (перебор 6 цифр)
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_email_codes_user   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_email_codes_purpose CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD'))
);
CREATE INDEX ix_email_codes_user_purpose ON email_codes (user_id, purpose, created_at DESC);

-- сменил пароль по коду — все входы на других устройствах закрываются
ALTER TABLE refresh_tokens DROP CONSTRAINT ck_refresh_tokens_reason;
ALTER TABLE refresh_tokens ADD CONSTRAINT ck_refresh_tokens_reason
    CHECK (revoked_reason IN ('ROTATED', 'LOGOUT', 'REUSE', 'PASSWORD_RESET'));

--rollback ALTER TABLE refresh_tokens DROP CONSTRAINT ck_refresh_tokens_reason;
--rollback ALTER TABLE refresh_tokens ADD CONSTRAINT ck_refresh_tokens_reason CHECK (revoked_reason IN ('ROTATED', 'LOGOUT', 'REUSE'));
--rollback DROP TABLE email_codes;
--rollback ALTER TABLE users DROP COLUMN email_verified_at;
