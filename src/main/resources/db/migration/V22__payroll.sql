-- ============================================================
-- PAYROLL
--
-- No tax rates of any kind are seeded by this migration - see
-- TaxConfiguration/TaxBracket's javadoc. A deploying organization must
-- configure current, correct rates from an authoritative source before
-- running real payroll. Nothing in this module is tax or legal advice.
-- ============================================================

CREATE TABLE pay_periods (
    id UUID PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    start_date DATE NOT NULL,

    end_date DATE NOT NULL,

    pay_date DATE NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_pay_periods_name
        UNIQUE (name),

    CONSTRAINT chk_pay_periods_period
        CHECK (end_date >= start_date),

    CONSTRAINT chk_pay_periods_pay_date
        CHECK (pay_date >= end_date)
);

CREATE TABLE employee_payroll_profiles (
    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    base_salary NUMERIC(12, 2) NOT NULL,

    -- MONTHLY, BIWEEKLY, WEEKLY
    pay_frequency VARCHAR(20) NOT NULL,

    standard_hours_per_period NUMERIC(6, 2),

    overtime_hourly_rate NUMERIC(10, 2),

    -- Opaque reference only - never a full bank account number. See
    -- EmployeePayrollProfile's javadoc.
    bank_account_reference VARCHAR(100),

    tax_number VARCHAR(50),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_employee_payroll_profiles_employee_id
        UNIQUE (employee_id),

    CONSTRAINT chk_employee_payroll_profiles_base_salary
        CHECK (base_salary >= 0)
);

CREATE TABLE tax_configurations (
    id UUID PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    description VARCHAR(500),

    -- DEDUCTION (reduces employee net pay), EMPLOYER_CONTRIBUTION (employer cost only)
    line_item_type VARCHAR(30) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_tax_configurations_name
        UNIQUE (name)
);

CREATE TABLE tax_brackets (
    id UUID PRIMARY KEY,

    tax_configuration_id UUID NOT NULL
        REFERENCES tax_configurations (id)
        ON DELETE CASCADE,

    min_amount NUMERIC(12, 2) NOT NULL,

    -- NULL means no upper bound (the top bracket).
    max_amount NUMERIC(12, 2),

    rate_percent NUMERIC(6, 3) NOT NULL,

    effective_from DATE NOT NULL,

    effective_to DATE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tax_brackets_rate_range
        CHECK (rate_percent >= 0 AND rate_percent <= 100),

    CONSTRAINT chk_tax_brackets_bounds
        CHECK (max_amount IS NULL OR max_amount > min_amount)
);

CREATE INDEX idx_tax_brackets_tax_configuration_id
    ON tax_brackets (tax_configuration_id);

CREATE TABLE payroll_runs (
    id UUID PRIMARY KEY,

    pay_period_id UUID NOT NULL
        REFERENCES pay_periods (id),

    -- DRAFT, APPROVED, PAID
    status VARCHAR(20) NOT NULL,

    run_by_user_id UUID NOT NULL
        REFERENCES users (id),

    approved_by_user_id UUID
        REFERENCES users (id),

    approved_at TIMESTAMPTZ,

    paid_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_payroll_runs_pay_period_id
        UNIQUE (pay_period_id)
);

CREATE TABLE payroll_run_entries (
    id UUID PRIMARY KEY,

    payroll_run_id UUID NOT NULL
        REFERENCES payroll_runs (id),

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    total_earnings NUMERIC(12, 2) NOT NULL DEFAULT 0,

    total_deductions NUMERIC(12, 2) NOT NULL DEFAULT 0,

    total_employer_contributions NUMERIC(12, 2) NOT NULL DEFAULT 0,

    net_pay NUMERIC(12, 2) NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_payroll_run_entries_run_employee
        UNIQUE (payroll_run_id, employee_id)
);

CREATE INDEX idx_payroll_run_entries_payroll_run_id
    ON payroll_run_entries (payroll_run_id);

CREATE INDEX idx_payroll_run_entries_employee_id
    ON payroll_run_entries (employee_id);

CREATE TABLE payroll_line_items (
    id UUID PRIMARY KEY,

    payroll_run_entry_id UUID NOT NULL
        REFERENCES payroll_run_entries (id)
        ON DELETE CASCADE,

    -- EARNING, DEDUCTION, EMPLOYER_CONTRIBUTION
    type VARCHAR(30) NOT NULL,

    code VARCHAR(50) NOT NULL,

    description VARCHAR(500),

    amount NUMERIC(12, 2) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_payroll_line_items_amount_non_negative
        CHECK (amount >= 0)
);

CREATE INDEX idx_payroll_line_items_payroll_run_entry_id
    ON payroll_line_items (payroll_run_entry_id);


-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES
    ('10000000-0000-0000-0000-000000000061', 'PAYROLL_MANAGE', 'Manage payroll configuration, profiles and runs'),
    ('10000000-0000-0000-0000-000000000062', 'PAYROLL_READ', 'View own payroll profile and payslips');


-- ADMIN: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000061'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000062');

-- HR_MANAGER: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000061'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000062');

-- HR_OFFICER: running payroll is routine HR/payroll administration work

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000061'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000062');

-- EMPLOYEE: own payroll profile and payslips only

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000062');
