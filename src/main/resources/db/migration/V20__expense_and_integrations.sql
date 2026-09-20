-- ============================================================
-- EXPENSE MANAGEMENT
-- ============================================================

CREATE TABLE expense_categories (
    id UUID PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    description VARCHAR(500),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_expense_categories_name
        UNIQUE (name)
);

CREATE TABLE expense_claims (
    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    category_id UUID NOT NULL
        REFERENCES expense_categories (id),

    amount NUMERIC(12, 2) NOT NULL,

    currency VARCHAR(3) NOT NULL,

    description VARCHAR(1000),

    expense_date DATE NOT NULL,

    receipt_document_id UUID
        REFERENCES documents (id),

    -- DRAFT, SUBMITTED, APPROVED, REJECTED, REIMBURSED
    status VARCHAR(20) NOT NULL,

    submitted_at TIMESTAMPTZ,

    approved_by UUID
        REFERENCES users (id),

    approved_at TIMESTAMPTZ,

    rejected_by UUID
        REFERENCES users (id),

    rejected_at TIMESTAMPTZ,

    rejection_reason VARCHAR(1000),

    reimbursed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_expense_claims_amount_positive
        CHECK (amount > 0)
);

CREATE INDEX idx_expense_claims_employee_id
    ON expense_claims (employee_id);

CREATE INDEX idx_expense_claims_status
    ON expense_claims (status);

CREATE INDEX idx_expense_claims_expense_date
    ON expense_claims (expense_date);


-- ============================================================
-- INTEGRATIONS FRAMEWORK
--
-- One row per known integration point (see IntegrationConfig's
-- TYPE_* constants), seeded below - this table is updated, never
-- created through, at runtime.
-- ============================================================

CREATE TABLE integration_configs (
    id UUID PRIMARY KEY,

    type VARCHAR(30) NOT NULL,

    provider_name VARCHAR(100) NOT NULL,

    enabled BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_integration_configs_type
        UNIQUE (type)
);

CREATE TABLE integration_settings (
    id UUID PRIMARY KEY,

    integration_config_id UUID NOT NULL
        REFERENCES integration_configs (id)
        ON DELETE CASCADE,

    setting_key VARCHAR(100) NOT NULL,

    -- Non-sensitive configuration only - see IntegrationSetting's javadoc.
    setting_value VARCHAR(500),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_integration_settings_config_key
        UNIQUE (integration_config_id, setting_key)
);

-- Seed the known integration points. EMAIL/STORAGE/ESIGNATURE are
-- already real, wired-up internal integrations from earlier phases;
-- PAYROLL/CALENDAR/IDENTITY_PROVIDER/EXTERNAL_HR_SYSTEM are registered
-- but not yet backed by a concrete external provider.

INSERT INTO integration_configs (id, type, provider_name, enabled)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'EMAIL', 'internal-smtp', TRUE),
    ('20000000-0000-0000-0000-000000000002', 'STORAGE', 'local-filesystem', TRUE),
    ('20000000-0000-0000-0000-000000000003', 'ESIGNATURE', 'internal', TRUE),
    ('20000000-0000-0000-0000-000000000004', 'PAYROLL', 'none', FALSE),
    ('20000000-0000-0000-0000-000000000005', 'CALENDAR', 'none', FALSE),
    ('20000000-0000-0000-0000-000000000006', 'IDENTITY_PROVIDER', 'none', FALSE),
    ('20000000-0000-0000-0000-000000000007', 'EXTERNAL_HR_SYSTEM', 'none', FALSE);


-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES
    ('10000000-0000-0000-0000-000000000056', 'EXPENSE_MANAGE', 'Manage expense categories, approve/reject/reimburse claims'),
    ('10000000-0000-0000-0000-000000000057', 'EXPENSE_READ', 'Submit and view own expense claims'),
    ('10000000-0000-0000-0000-000000000058', 'INTEGRATION_MANAGE', 'View and configure platform integrations');


-- ADMIN: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000056'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000057'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000058');

-- HR_MANAGER: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000056'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000057'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000058');

-- HR_OFFICER: expense administration is routine HR work, but platform
-- integration configuration is not - same split as CALENDAR_MANAGE_HOLIDAYS/
-- SCHEDULE_MANAGE in Phase 5.

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000056'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000057');

-- EMPLOYEE: own expense claims only

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000057');
