-- ============================================================
-- RECRUITMENT / ATS
-- ============================================================

CREATE TABLE job_requisitions (
    id UUID PRIMARY KEY,

    title VARCHAR(200) NOT NULL,

    department_id UUID NOT NULL
        REFERENCES departments (id),

    description VARCHAR(2000),

    number_of_openings INTEGER NOT NULL DEFAULT 1,

    -- OPEN, ON_HOLD, CLOSED
    status VARCHAR(20) NOT NULL,

    requested_by UUID NOT NULL
        REFERENCES users (id),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_job_requisitions_openings_positive
        CHECK (number_of_openings > 0)
);

CREATE INDEX idx_job_requisitions_department_id
    ON job_requisitions (department_id);

CREATE TABLE job_postings (
    id UUID PRIMARY KEY,

    requisition_id UUID NOT NULL
        REFERENCES job_requisitions (id),

    title VARCHAR(200) NOT NULL,

    description VARCHAR(4000),

    location VARCHAR(200),

    -- FULL_TIME, PART_TIME, CONTRACT, TEMPORARY
    employment_type VARCHAR(20) NOT NULL,

    -- OPEN, CLOSED
    status VARCHAR(20) NOT NULL,

    published_at TIMESTAMPTZ NOT NULL,

    closed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_job_postings_requisition_id
    ON job_postings (requisition_id);

CREATE INDEX idx_job_postings_status
    ON job_postings (status);

CREATE TABLE candidates (
    id UUID PRIMARY KEY,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100) NOT NULL,

    email VARCHAR(255) NOT NULL,

    phone VARCHAR(30),

    resume_document_id UUID
        REFERENCES documents (id),

    source VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_candidates_email
        UNIQUE (email)
);

CREATE TABLE applications (
    id UUID PRIMARY KEY,

    candidate_id UUID NOT NULL
        REFERENCES candidates (id),

    job_posting_id UUID NOT NULL
        REFERENCES job_postings (id),

    -- APPLIED, SCREENING, INTERVIEWING, OFFER, HIRED, REJECTED, WITHDRAWN
    status VARCHAR(20) NOT NULL,

    rejection_reason VARCHAR(1000),

    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_applications_candidate_posting
        UNIQUE (candidate_id, job_posting_id)
);

CREATE INDEX idx_applications_job_posting_id
    ON applications (job_posting_id);

CREATE INDEX idx_applications_candidate_id
    ON applications (candidate_id);

CREATE TABLE interviews (
    id UUID PRIMARY KEY,

    application_id UUID NOT NULL
        REFERENCES applications (id),

    interviewer_user_id UUID NOT NULL
        REFERENCES users (id),

    scheduled_at TIMESTAMPTZ NOT NULL,

    duration_minutes INTEGER NOT NULL,

    location VARCHAR(200),

    -- SCHEDULED, COMPLETED, CANCELLED
    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interviews_application_id
    ON interviews (application_id);

CREATE INDEX idx_interviews_interviewer_user_id
    ON interviews (interviewer_user_id);

CREATE TABLE interview_feedback (
    id UUID PRIMARY KEY,

    interview_id UUID NOT NULL
        REFERENCES interviews (id),

    interviewer_user_id UUID NOT NULL
        REFERENCES users (id),

    rating INTEGER,

    comments VARCHAR(2000),

    -- HIRE, NO_HIRE, MAYBE
    recommendation VARCHAR(20) NOT NULL,

    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_interview_feedback_interview_id
        UNIQUE (interview_id)
);

CREATE TABLE offers (
    id UUID PRIMARY KEY,

    application_id UUID NOT NULL
        REFERENCES applications (id),

    job_title VARCHAR(200) NOT NULL,

    salary_amount NUMERIC(12, 2) NOT NULL,

    currency VARCHAR(3) NOT NULL,

    start_date DATE NOT NULL,

    -- DRAFT, SENT, ACCEPTED, DECLINED, WITHDRAWN
    status VARCHAR(20) NOT NULL,

    sent_at TIMESTAMPTZ,

    responded_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_offers_application_id
        UNIQUE (application_id),

    CONSTRAINT chk_offers_salary_positive
        CHECK (salary_amount > 0)
);


-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES
    ('10000000-0000-0000-0000-000000000059', 'RECRUITMENT_MANAGE', 'Manage requisitions, postings, candidates, applications, interviews and offers'),
    ('10000000-0000-0000-0000-000000000060', 'RECRUITMENT_INTERVIEWER', 'View own assigned interviews and submit feedback for them');


-- ADMIN: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000059'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000060');

-- HR_MANAGER: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000059'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000060');

-- HR_OFFICER: routine HR work - manage the pipeline too

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000059'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000060');

-- EMPLOYEE: can be tapped as an interviewer, but cannot manage the pipeline

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000060');
