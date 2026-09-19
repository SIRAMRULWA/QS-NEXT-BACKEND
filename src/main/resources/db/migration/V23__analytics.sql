-- ============================================================
-- ANALYTICS
--
-- The analytics module is read-only and introduces no new tables of
-- its own - it aggregates existing data with real SQL-level
-- COUNT/SUM/GROUP BY queries (interface projections), never by
-- fetching whole tables into memory. See AnalyticsService's javadoc
-- for the one deliberate approximation it makes (employee turnover).
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES
    ('10000000-0000-0000-0000-000000000063', 'ANALYTICS_READ', 'View organization-wide analytics dashboards');


-- ADMIN: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000063');

-- HR_MANAGER: all of the above

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000063');

-- HR_OFFICER: org-wide analytics dashboards are routine HR reporting work,
-- same tier as REPORT_READ

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000063');

-- EMPLOYEE: not granted - analytics is org-wide/cross-employee, not
-- self-service data
