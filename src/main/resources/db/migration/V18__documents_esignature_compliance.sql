-- ============================================================
-- DOCUMENT MANAGEMENT
-- ============================================================

CREATE TABLE documents (
    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    -- CONTRACT, ID_DOCUMENT, QUALIFICATION, POLICY_ACKNOWLEDGEMENT, TAX_FORM, OTHER, ...
    category VARCHAR(50) NOT NULL,

    title VARCHAR(200) NOT NULL,

    description VARCHAR(1000),

    -- Groups every version of "the same" document together.
    document_family_id UUID NOT NULL,

    version INTEGER NOT NULL,

    -- ACTIVE (current version), SUPERSEDED (replaced by a newer version), ARCHIVED
    status VARCHAR(20) NOT NULL,

    -- Opaque key into DocumentStorageService - not a filesystem path
    -- guarantee, just what the configured storage implementation uses
    -- to address these bytes.
    storage_key VARCHAR(500) NOT NULL,

    original_filename VARCHAR(255) NOT NULL,

    content_type VARCHAR(100) NOT NULL,

    file_size_bytes BIGINT NOT NULL,

    expiry_date DATE,

    uploaded_by UUID NOT NULL
        REFERENCES users (id),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_documents_family_version
        UNIQUE (document_family_id, version)
);

CREATE INDEX idx_documents_employee_id
    ON documents (employee_id);

CREATE INDEX idx_documents_document_family_id
    ON documents (document_family_id);

CREATE INDEX idx_documents_expiry_date
    ON documents (expiry_date);


-- ============================================================
-- E-SIGNATURE
--
-- Self-hosted request/accept/reject workflow only - see
-- ESignatureProvider for the extension point a real external
-- e-signature vendor would plug into for a legally binding signature.
-- ============================================================

CREATE TABLE signature_requests (
    id UUID PRIMARY KEY,

    document_id UUID NOT NULL
        REFERENCES documents (id),

    title VARCHAR(200) NOT NULL,

    requested_by UUID NOT NULL
        REFERENCES users (id),

    -- PENDING, COMPLETED, DECLINED, CANCELLED, EXPIRED
    status VARCHAR(20) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,

    completed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_signature_requests_document_id
    ON signature_requests (document_id);

CREATE TABLE signature_request_signers (
    id UUID PRIMARY KEY,

    signature_request_id UUID NOT NULL
        REFERENCES signature_requests (id)
        ON DELETE CASCADE,

    signer_user_id UUID NOT NULL
        REFERENCES users (id),

    -- PENDING, SIGNED, DECLINED
    status VARCHAR(20) NOT NULL,

    signed_at TIMESTAMPTZ,

    decline_reason VARCHAR(500),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_signature_request_signers_request_signer
        UNIQUE (signature_request_id, signer_user_id)
);

CREATE INDEX idx_signature_request_signers_request_id
    ON signature_request_signers (signature_request_id);

CREATE INDEX idx_signature_request_signers_signer_user_id
    ON signature_request_signers (signer_user_id);


-- ============================================================
-- COMPLIANCE
--
-- Requirements are configuration, not hardcoded legal conclusions -
-- category/validity are set per requirement, not baked into code.
-- ============================================================

CREATE TABLE compliance_requirements (
    id UUID PRIMARY KEY,

    name VARCHAR(150) NOT NULL,

    description VARCHAR(1000),

    -- DOCUMENT, POLICY_ACKNOWLEDGEMENT, TRAINING, OTHER
    category VARCHAR(30) NOT NULL,

    mandatory BOOLEAN NOT NULL DEFAULT TRUE,

    -- How long a completed record stays valid before it must be
    -- renewed. NULL means it never expires once met.
    validity_period_days INTEGER,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_compliance_requirements_name
        UNIQUE (name)
);

CREATE TABLE compliance_records (
    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL
        REFERENCES employees (id),

    requirement_id UUID NOT NULL
        REFERENCES compliance_requirements (id),

    -- PENDING, COMPLETED, EXPIRED
    status VARCHAR(20) NOT NULL,

    -- Audit evidence: the Document (if any) that supports this record,
    -- e.g. the uploaded ID document or signed policy acknowledgement.
    evidence_document_id UUID
        REFERENCES documents (id),

    notes VARCHAR(1000),

    completed_at TIMESTAMPTZ,

    expires_at TIMESTAMPTZ,

    reviewed_by UUID
        REFERENCES users (id),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_compliance_records_employee_id
    ON compliance_records (employee_id);

CREATE INDEX idx_compliance_records_requirement_id
    ON compliance_records (requirement_id);

CREATE INDEX idx_compliance_records_expires_at
    ON compliance_records (expires_at);


-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES
    ('10000000-0000-0000-0000-000000000044', 'DOCUMENT_READ', 'View own documents'),
    ('10000000-0000-0000-0000-000000000045', 'DOCUMENT_MANAGE', 'Upload and manage any employee''s documents'),
    ('10000000-0000-0000-0000-000000000046', 'ESIGNATURE_MANAGE', 'Create and cancel signature requests'),
    ('10000000-0000-0000-0000-000000000047', 'ESIGNATURE_SIGN', 'Accept or decline a signature request as a signer'),
    ('10000000-0000-0000-0000-000000000048', 'COMPLIANCE_MANAGE', 'Manage compliance requirements and records'),
    ('10000000-0000-0000-0000-000000000049', 'COMPLIANCE_READ', 'View and complete own compliance records');


-- ADMIN: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000044'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000045'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000046'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000047'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000048'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000049');

-- HR_MANAGER: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000044'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000045'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000046'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000047'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000048'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000049');

-- HR_OFFICER: this is routine day-to-day HR admin work, same tier as
-- ONBOARDING_MANAGE in the core HR expansion - all of the above too.

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000044'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000045'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000046'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000047'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000048'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000049');

-- EMPLOYEE: own documents, act as a signer, own compliance records

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000044'),
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000047'),
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000049');
