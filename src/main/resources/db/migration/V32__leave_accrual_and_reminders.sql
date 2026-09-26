-- ============================================================
-- LEAVE ACCRUAL AND REMINDERS
--
--   * leave_policies: per leave type, how many days a year an active
--     employee earns, whether they accrue monthly or up front, and how
--     many unused days carry into the next year;
--   * leave_accrual_runs: one row per policy per period, so a scheduled
--     or manual accrual run never grants the same month twice;
--   * reminder_log: one row per reminder sent, so the daily reminder job
--     never sends the same reminder twice.
-- ============================================================

CREATE TABLE leave_policies (
    id UUID PRIMARY KEY,

    leave_type VARCHAR(30) NOT NULL,

    annual_days NUMERIC(6, 2) NOT NULL,

    -- MONTHLY: annual_days / 12 each month. ANNUAL: all of it in January
    -- (or when an employee's balance is first created that year).
    accrual_method VARCHAR(20) NOT NULL,

    carry_over_max_days NUMERIC(6, 2) NOT NULL DEFAULT 0,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_leave_policies_type
        UNIQUE (leave_type),

    CONSTRAINT chk_leave_policies_type
        CHECK (
            leave_type IN (
                'ANNUAL', 'SICK', 'FAMILY_RESPONSIBILITY', 'MATERNITY',
                'PATERNITY', 'UNPAID', 'STUDY', 'OTHER'
            )
        ),

    CONSTRAINT chk_leave_policies_method
        CHECK (accrual_method IN ('MONTHLY', 'ANNUAL')),

    CONSTRAINT chk_leave_policies_days
        CHECK (annual_days >= 0 AND carry_over_max_days >= 0)
);

CREATE TABLE leave_accrual_runs (
    id UUID PRIMARY KEY,

    policy_id UUID NOT NULL
        REFERENCES leave_policies (id),

    -- 'YYYY-MM'
    period VARCHAR(7) NOT NULL,

    employees_credited INTEGER NOT NULL,

    run_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_leave_accrual_runs_policy_period
        UNIQUE (policy_id, period)
);

CREATE TABLE reminder_log (
    reminder_key VARCHAR(200) PRIMARY KEY,

    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
