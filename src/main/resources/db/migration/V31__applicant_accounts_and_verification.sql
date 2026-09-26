-- ============================================================
-- APPLICANT ACCOUNTS AND EMAIL VERIFICATION
--
--   * candidates.user_id links a candidate to the self-signup login that
--     applied, so the applicant can track their own applications and a
--     hire reuses that login instead of creating a second account;
--   * users.email_verified gates applying until the signup address is
--     confirmed. Existing accounts are treated as verified.
-- ============================================================

ALTER TABLE candidates
    ADD COLUMN user_id UUID
        REFERENCES users (id);

CREATE UNIQUE INDEX uk_candidates_user_id
    ON candidates (user_id)
    WHERE user_id IS NOT NULL;

ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL
        REFERENCES users (id),

    token_hash VARCHAR(255) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,

    used_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_email_verification_tokens_token_hash
        UNIQUE (token_hash)
);

CREATE INDEX idx_email_verification_tokens_user_id
    ON email_verification_tokens (user_id);
