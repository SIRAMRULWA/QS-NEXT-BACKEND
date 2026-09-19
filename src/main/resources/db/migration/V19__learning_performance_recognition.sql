-- ============================================================
-- LEARNING & SKILLS
-- ============================================================

CREATE TABLE skills (
    id UUID PRIMARY KEY,

    name VARCHAR(150) NOT NULL,

    category VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_skills_name
        UNIQUE (name)
);

CREATE TABLE employee_skills (
    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    skill_id UUID NOT NULL
        REFERENCES skills (id),

    -- BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    proficiency_level VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_employee_skills_employee_skill
        UNIQUE (employee_id, skill_id)
);

CREATE INDEX idx_employee_skills_employee_id
    ON employee_skills (employee_id);

CREATE TABLE courses (
    id UUID PRIMARY KEY,

    title VARCHAR(200) NOT NULL,

    description VARCHAR(2000),

    category VARCHAR(100),

    duration_minutes INTEGER,

    mandatory BOOLEAN NOT NULL DEFAULT FALSE,

    -- Ties course completion back to a compliance requirement - see
    -- ComplianceService#autoCompleteFromTraining.
    linked_compliance_requirement_id UUID
        REFERENCES compliance_requirements (id),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_courses_title
        UNIQUE (title)
);

CREATE TABLE learning_paths (
    id UUID PRIMARY KEY,

    name VARCHAR(150) NOT NULL,

    description VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_learning_paths_name
        UNIQUE (name)
);

CREATE TABLE learning_path_courses (
    id UUID PRIMARY KEY,

    learning_path_id UUID NOT NULL
        REFERENCES learning_paths (id)
        ON DELETE CASCADE,

    course_id UUID NOT NULL
        REFERENCES courses (id),

    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_learning_path_courses_learning_path_id
    ON learning_path_courses (learning_path_id);

CREATE TABLE course_enrollments (
    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    course_id UUID NOT NULL
        REFERENCES courses (id),

    -- ENROLLED, IN_PROGRESS, COMPLETED
    status VARCHAR(20) NOT NULL,

    progress_percent INTEGER NOT NULL DEFAULT 0,

    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    completed_at TIMESTAMPTZ,

    certificate_issued_at TIMESTAMPTZ,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_course_enrollments_employee_course
        UNIQUE (employee_id, course_id),

    CONSTRAINT chk_course_enrollments_progress_percent
        CHECK (progress_percent BETWEEN 0 AND 100)
);

CREATE INDEX idx_course_enrollments_employee_id
    ON course_enrollments (employee_id);


-- ============================================================
-- PERFORMANCE MANAGEMENT
-- ============================================================

CREATE TABLE performance_cycles (
    id UUID PRIMARY KEY,

    name VARCHAR(150) NOT NULL,

    start_date DATE NOT NULL,

    end_date DATE NOT NULL,

    -- OPEN, CLOSED
    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_performance_cycles_name
        UNIQUE (name),

    CONSTRAINT chk_performance_cycles_period
        CHECK (end_date >= start_date)
);

CREATE TABLE performance_goals (
    id UUID PRIMARY KEY,

    cycle_id UUID NOT NULL
        REFERENCES performance_cycles (id),

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    title VARCHAR(200) NOT NULL,

    description VARCHAR(1000),

    target_date DATE,

    -- NOT_STARTED, IN_PROGRESS, COMPLETED
    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_performance_goals_employee_id
    ON performance_goals (employee_id);

CREATE TABLE performance_reviews (
    id UUID PRIMARY KEY,

    cycle_id UUID NOT NULL
        REFERENCES performance_cycles (id),

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    reviewer_user_id UUID NOT NULL
        REFERENCES users (id),

    -- DRAFT, IN_PROGRESS, COMPLETED
    status VARCHAR(20) NOT NULL,

    self_rating INTEGER,

    self_comments VARCHAR(2000),

    self_submitted_at TIMESTAMPTZ,

    manager_rating INTEGER,

    manager_comments VARCHAR(2000),

    manager_submitted_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_performance_reviews_cycle_employee
        UNIQUE (cycle_id, employee_id)
);

CREATE INDEX idx_performance_reviews_employee_id
    ON performance_reviews (employee_id);

CREATE INDEX idx_performance_reviews_reviewer_user_id
    ON performance_reviews (reviewer_user_id);

CREATE TABLE development_plans (
    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    review_id UUID
        REFERENCES performance_reviews (id),

    description VARCHAR(2000) NOT NULL,

    recommended_course_id UUID
        REFERENCES courses (id),

    target_date DATE,

    -- OPEN, COMPLETED
    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_development_plans_employee_id
    ON development_plans (employee_id);


-- ============================================================
-- RECOGNITION
-- ============================================================

CREATE TABLE recognition_types (
    id UUID PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    description VARCHAR(500),

    point_value INTEGER NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_recognition_types_name
        UNIQUE (name)
);

CREATE TABLE recognitions (
    id UUID PRIMARY KEY,

    type_id UUID NOT NULL
        REFERENCES recognition_types (id),

    given_by_user_id UUID NOT NULL
        REFERENCES users (id),

    given_to_employee_id UUID NOT NULL
        REFERENCES employees (id),

    message VARCHAR(1000),

    -- Snapshot of the type's point value at the time this was given.
    points INTEGER NOT NULL,

    -- PUBLIC, PRIVATE
    visibility VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recognitions_given_to_employee_id
    ON recognitions (given_to_employee_id);

CREATE INDEX idx_recognitions_given_by_user_id
    ON recognitions (given_by_user_id);


-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES
    ('10000000-0000-0000-0000-000000000050', 'LEARNING_MANAGE', 'Manage courses, learning paths and the skills catalog'),
    ('10000000-0000-0000-0000-000000000051', 'LEARNING_READ', 'Browse courses, enrol and track own learning'),
    ('10000000-0000-0000-0000-000000000052', 'PERFORMANCE_MANAGE', 'Manage performance cycles, goals and reviews'),
    ('10000000-0000-0000-0000-000000000053', 'PERFORMANCE_READ', 'View and act on own performance records'),
    ('10000000-0000-0000-0000-000000000054', 'RECOGNITION_GIVE', 'Give recognition to a colleague'),
    ('10000000-0000-0000-0000-000000000055', 'RECOGNITION_MANAGE', 'Manage the recognition type catalog');


-- ADMIN: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000050'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000051'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000052'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000053'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000054'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000055');

-- HR_MANAGER: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000050'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000051'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000052'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000053'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000054'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000055');

-- HR_OFFICER: routine HR admin work - all of the above too

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000050'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000051'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000052'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000053'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000054'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000055');

-- EMPLOYEE: own learning/performance data, plus giving peer recognition

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000051'),
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000053'),
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000054');
