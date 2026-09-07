CREATE TABLE emails (
    id UUID PRIMARY KEY,

    recipient VARCHAR(255) NOT NULL,

    subject VARCHAR(255) NOT NULL,

    body TEXT NOT NULL,

    type VARCHAR(50) NOT NULL,

    status VARCHAR(20) NOT NULL,

    attempt_count INTEGER NOT NULL DEFAULT 0,

    last_attempt_at TIMESTAMPTZ,

    sent_at TIMESTAMPTZ,

    failure_reason VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_emails_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'SENT',
                'FAILED'
            )
        ),

    CONSTRAINT chk_emails_attempt_count
        CHECK (
            attempt_count >= 0
        )
);
