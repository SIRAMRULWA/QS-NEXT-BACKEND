-- ============================================================
-- USER ROLE MANAGEMENT
-- Grants an ADMIN the ability to assign/remove roles from other user
-- accounts (previously the only way to change a user's role was a direct
-- SQL statement against the database - there was no API for it at all).
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES (
    '10000000-0000-0000-0000-000000000067',
    'USER_ROLE_MANAGE',
    'Assign or remove roles from user accounts'
);

-- ADMIN only - granting roles (including ADMIN itself) is the most
-- sensitive action in the system.
INSERT INTO role_permissions (role_id, permission_id)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000067'
);
