-- ============================================================
-- MOBILE
--
-- No new permissions - the dashboard endpoint only ever returns data
-- the caller already has an existing authority for (EMPLOYEE_READ,
-- ATTENDANCE_READ, LEAVE_READ, CALENDAR_READ, NOTIFICATION_READ, all
-- already granted to every role including EMPLOYEE - see V13/V17),
-- and device registration is a self-scoped action needing no domain
-- permission of its own, gated by isAuthenticated() only.
--
-- This table is the device-token registry PushNotificationSender's
-- javadoc has been pointing to since Phase 4 - see
-- DeviceRegistryPushNotificationSender for the first real
-- implementation of that interface.
-- ============================================================

CREATE TABLE mobile_devices (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL
        REFERENCES users (id),

    -- Opaque FCM/APNs/web-push registration token.
    device_token VARCHAR(500) NOT NULL,

    -- IOS, ANDROID, WEB
    platform VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_mobile_devices_device_token
        UNIQUE (device_token),

    CONSTRAINT chk_mobile_devices_platform
        CHECK (platform IN ('IOS', 'ANDROID', 'WEB'))
);

CREATE INDEX idx_mobile_devices_user_id
    ON mobile_devices (user_id);
