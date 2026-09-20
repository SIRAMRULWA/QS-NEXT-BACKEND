-- Phase 17 DB audit: foreign-key columns that are actually filtered on by a
-- repository query (or, for onboarding_workflows/shift_assignments, are the
-- obvious join key for looking up a template's/shift's dependents) but had
-- no index - Postgres does not create one for a FK automatically, and none
-- of these were covered by another index or by a unique constraint that
-- already leads with this column.

CREATE INDEX idx_onboarding_workflows_template_id
    ON onboarding_workflows (template_id);

CREATE INDEX idx_shift_assignments_shift_id
    ON shift_assignments (shift_id);

CREATE INDEX idx_employee_skills_skill_id
    ON employee_skills (skill_id);

CREATE INDEX idx_course_enrollments_course_id
    ON course_enrollments (course_id);

CREATE INDEX idx_performance_goals_cycle_id
    ON performance_goals (cycle_id);

CREATE INDEX idx_expense_claims_category_id
    ON expense_claims (category_id);
