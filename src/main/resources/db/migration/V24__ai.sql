-- ============================================================
-- AI
--
-- AI is a SAFE abstraction layer, not a hardcoded dependency on one AI
-- vendor - see AiProvider's javadoc. This migration only registers the
-- AI integration point (disabled by default) and a table to keep every
-- AI-generated response reviewable by an authorized human afterwards.
-- No vendor credentials are stored here - those come from an
-- environment variable, per the project's secrets policy.
-- ============================================================

INSERT INTO integration_configs (id, type, provider_name, enabled)
VALUES
    ('20000000-0000-0000-0000-000000000008', 'AI', 'anthropic', false);

CREATE TABLE ai_suggestions (
    id UUID PRIMARY KEY,

    -- HR_ASSISTANT, DOCUMENT_SUMMARY, JOB_DESCRIPTION, CANDIDATE_MATCH,
    -- SKILLS_RECOMMENDATION, LEARNING_RECOMMENDATION, ANALYTICS_EXPLANATION
    type VARCHAR(40) NOT NULL,

    -- Optional pointer to the entity this suggestion was about
    -- (e.g. "Employee", "Document", "JobPosting") - no foreign key,
    -- since the subject type varies by row.
    subject_type VARCHAR(40),
    subject_id UUID,

    requested_by_user_id UUID NOT NULL
        REFERENCES users (id),

    provider_name VARCHAR(50) NOT NULL,

    prompt TEXT NOT NULL,

    response_text TEXT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_suggestions_requested_by_user_id
    ON ai_suggestions (requested_by_user_id);

CREATE INDEX idx_ai_suggestions_subject
    ON ai_suggestions (subject_type, subject_id);


-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES
    ('10000000-0000-0000-0000-000000000064', 'AI_ASSISTANT_USE', 'Ask the HR assistant general questions'),
    ('10000000-0000-0000-0000-000000000065', 'AI_RECOMMENDATIONS_READ', 'Request AI skills/learning recommendations for oneself'),
    ('10000000-0000-0000-0000-000000000066', 'AI_HR_TOOLS_USE', 'Use HR-facing AI tools: document summarization, job description drafting, candidate matching, analytics explanations');


-- ADMIN: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000064'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000065'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000066');

-- HR_MANAGER: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000064'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000065'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000066');

-- HR_OFFICER: HR-facing AI tools are routine HR work, same tier as
-- RECRUITMENT_MANAGE and ANALYTICS_READ

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000064'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000065'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000066');

-- EMPLOYEE: self-service only - the HR assistant and recommendations
-- for themselves, never the HR-facing tools (other people's documents,
-- recruitment data, org-wide analytics)

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000064'),
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000065');
