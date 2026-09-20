-- ============================================================
-- EMPLOYEE MANAGER (self-referencing) - needed by the employee
-- directory's manager hierarchy; was missing from the original
-- employees table.
-- ============================================================

ALTER TABLE employees
    ADD COLUMN manager_id UUID
        REFERENCES employees (id);

CREATE INDEX idx_employees_manager_id
    ON employees (manager_id);


-- ============================================================
-- CALENDAR
-- ============================================================

CREATE TABLE calendar_events (
    id UUID PRIMARY KEY,

    title VARCHAR(200) NOT NULL,

    description VARCHAR(2000),

    start_at TIMESTAMPTZ NOT NULL,

    end_at TIMESTAMPTZ NOT NULL,

    all_day BOOLEAN NOT NULL DEFAULT FALSE,

    -- MEETING, COMPANY, HOLIDAY, LEAVE, SHIFT
    event_type VARCHAR(30) NOT NULL,

    -- PUBLIC (whole company), DEPARTMENT, PRIVATE (owner only)
    visibility VARCHAR(20) NOT NULL,

    owner_user_id UUID
        REFERENCES users (id),

    department_id UUID
        REFERENCES departments (id),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_calendar_events_period
        CHECK (end_at >= start_at)
);

CREATE INDEX idx_calendar_events_start_at
    ON calendar_events (start_at);

CREATE INDEX idx_calendar_events_owner_user_id
    ON calendar_events (owner_user_id);

CREATE INDEX idx_calendar_events_department_id
    ON calendar_events (department_id);


-- ============================================================
-- ONBOARDING
-- ============================================================

CREATE TABLE onboarding_templates (
    id UUID PRIMARY KEY,

    name VARCHAR(150) NOT NULL,

    description VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_onboarding_templates_name
        UNIQUE (name)
);

CREATE TABLE onboarding_template_tasks (
    id UUID PRIMARY KEY,

    template_id UUID NOT NULL
        REFERENCES onboarding_templates (id)
        ON DELETE CASCADE,

    title VARCHAR(200) NOT NULL,

    description VARCHAR(1000),

    -- HR, MANAGER, EMPLOYEE
    assignee_role VARCHAR(20) NOT NULL,

    sort_order INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_onboarding_template_tasks_template_id
    ON onboarding_template_tasks (template_id);

CREATE TABLE onboarding_workflows (
    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    template_id UUID NOT NULL
        REFERENCES onboarding_templates (id),

    -- IN_PROGRESS, COMPLETED
    status VARCHAR(20) NOT NULL,

    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    completed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_onboarding_workflows_employee_id
    ON onboarding_workflows (employee_id);

CREATE TABLE onboarding_tasks (
    id UUID PRIMARY KEY,

    workflow_id UUID NOT NULL
        REFERENCES onboarding_workflows (id)
        ON DELETE CASCADE,

    title VARCHAR(200) NOT NULL,

    description VARCHAR(1000),

    assignee_role VARCHAR(20) NOT NULL,

    -- PENDING, COMPLETED
    status VARCHAR(20) NOT NULL,

    sort_order INTEGER NOT NULL DEFAULT 0,

    completed_at TIMESTAMPTZ,

    completed_by UUID
        REFERENCES users (id),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_onboarding_tasks_workflow_id
    ON onboarding_tasks (workflow_id);


-- ============================================================
-- SCHEDULING
-- ============================================================

CREATE TABLE shifts (
    id UUID PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    start_time TIME NOT NULL,

    end_time TIME NOT NULL,

    department_id UUID
        REFERENCES departments (id),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shift_assignments (
    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    shift_id UUID NOT NULL
        REFERENCES shifts (id),

    work_date DATE NOT NULL,

    -- SCHEDULED, CANCELLED
    status VARCHAR(20) NOT NULL,

    created_by UUID NOT NULL
        REFERENCES users (id),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shift_assignments_employee_id
    ON shift_assignments (employee_id);

CREATE INDEX idx_shift_assignments_work_date
    ON shift_assignments (work_date);

-- Only one *active* (SCHEDULED) assignment per employee per day - a
-- plain UNIQUE(employee_id, work_date) would also block re-assigning a
-- day after its original assignment was CANCELLED, which the service
-- layer (ScheduleService#assignShift) explicitly allows.
CREATE UNIQUE INDEX uk_shift_assignments_employee_active_work_date
    ON shift_assignments (employee_id, work_date)
    WHERE status = 'SCHEDULED';


-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES
    ('10000000-0000-0000-0000-000000000036', 'DIRECTORY_READ', 'Browse the employee directory'),
    ('10000000-0000-0000-0000-000000000037', 'CALENDAR_READ', 'View calendar events'),
    ('10000000-0000-0000-0000-000000000038', 'CALENDAR_CREATE', 'Create calendar events'),
    ('10000000-0000-0000-0000-000000000039', 'CALENDAR_MANAGE_HOLIDAYS', 'Manage company holidays'),
    ('10000000-0000-0000-0000-000000000040', 'ONBOARDING_MANAGE', 'Manage onboarding templates and workflows'),
    ('10000000-0000-0000-0000-000000000041', 'ONBOARDING_TASK_READ', 'View and complete own onboarding tasks'),
    ('10000000-0000-0000-0000-000000000042', 'SCHEDULE_MANAGE', 'Manage shifts and shift assignments'),
    ('10000000-0000-0000-0000-000000000043', 'SCHEDULE_READ', 'View schedules');


-- ADMIN: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000036'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000037'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000038'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000039'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000040'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000041'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000042'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000043');

-- HR_MANAGER: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000036'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000037'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000038'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000039'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000040'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000041'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000042'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000043');

-- HR_OFFICER: everything except managing holidays and shifts

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000036'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000037'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000038'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000040'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000041'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000043');

-- EMPLOYEE: browse, own calendar, own onboarding tasks, own schedule

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000036'),
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000037'),
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000038'),
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000041'),
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000043');
