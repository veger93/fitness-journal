--liquibase formatted sql

--changeset sergey:005-create-refresh-tokens
-- Refresh-токены: долгоживущие "пропуска" для получения нового короткого access-токена.
-- Храним НЕ сам токен, а его SHA-256: утечка таблицы не даст войти ни под кем (как пароли, только хеш быстрый —
-- токен и так случайный и длинный, перебирать его бессмысленно).
CREATE TABLE refresh_tokens (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL,
    token_hash     VARCHAR(64) NOT NULL,   -- SHA-256 в hex
    family_id      UUID        NOT NULL,   -- "сессия": все токены, выросшие из одного входа (с одного устройства)
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,
    revoked_reason VARCHAR(20),            -- ROTATED / LOGOUT / REUSE
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_tokens_reason CHECK (revoked_reason IN ('ROTATED', 'LOGOUT', 'REUSE'))
);

-- отзыв всей "сессии" разом (выход, подозрение на кражу) — по family_id
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
--rollback DROP TABLE refresh_tokens;
