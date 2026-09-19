ALTER TABLE users
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN locked_until TIMESTAMPTZ;

ALTER TABLE users
    ADD COLUMN last_login_at TIMESTAMPTZ;

ALTER TABLE users
    ADD CONSTRAINT chk_users_failed_login_attempts
        CHECK (failed_login_attempts >= 0);


-- ============================================================
-- REFRESH TOKENS
--
-- Server-side tracking for issued refresh tokens, keyed by the
-- JWT's "jti" claim, so tokens can be rotated and revoked before
-- they naturally expire.
-- ============================================================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL
        REFERENCES users (id),

    expires_at TIMESTAMPTZ NOT NULL,

    revoked_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_by_ip VARCHAR(64)
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens (expires_at);


-- ============================================================
-- PASSWORD RESET TOKENS
--
-- Only a SHA-256 hash of the opaque reset token is stored, never
-- the token itself.
-- ============================================================

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL
        REFERENCES users (id),

    token_hash VARCHAR(255) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,

    used_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_password_reset_tokens_token_hash
        UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_user_id
    ON password_reset_tokens (user_id);
