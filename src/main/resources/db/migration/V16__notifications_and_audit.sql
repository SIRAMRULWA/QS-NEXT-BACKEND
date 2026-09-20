-- ============================================================
-- NOTIFICATION PREFERENCES
-- ============================================================

CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL
        REFERENCES users (id),

    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_notification_preferences_user_id
        UNIQUE (user_id)
);


-- ============================================================
-- AUDIT READ PERMISSION
--
-- The audit trail can reveal who did what across the whole
-- platform, so read access is granted to ADMIN only by default.
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES
    ('10000000-0000-0000-0000-000000000035',
     'AUDIT_READ',
     'View audit log entries');

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000001',
     '10000000-0000-0000-0000-000000000035');
