-- ============================================================
-- LEAST-PRIVILEGE ROLES
--
-- Until now self-signup handed out EMPLOYEE, and EMPLOYEE could list
-- every employee, browse the directory and departments, allocate leave
-- days to anyone and see "My Interviews". This migration:
--
--   * adds APPLICANT (the new self-signup role), MANAGER (approves their
--     direct reports' requests) and INTERVIEWER (granted when HR assigns
--     someone to an interview);
--   * adds LEAVE_ALLOCATE so only HR/admin can create leave balances;
--   * trims EMPLOYEE down to self-service;
--   * moves existing accounts onto the right roles.
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES
    ('10000000-0000-0000-0000-000000000068', 'LEAVE_ALLOCATE', 'Allocate and adjust employees'' leave balances'),
    ('10000000-0000-0000-0000-000000000069', 'JOB_BOARD_READ', 'View published, open job postings'),
    ('10000000-0000-0000-0000-000000000070', 'APPLICATION_SELF', 'Apply for jobs and track own applications'),
    ('10000000-0000-0000-0000-000000000071', 'EXPENSE_APPROVE', 'Approve or reject direct reports'' expense claims'),
    ('10000000-0000-0000-0000-000000000072', 'TEAM_READ', 'View own direct reports and their pending requests');

INSERT INTO roles (id, name, description)
VALUES
    ('00000000-0000-0000-0000-000000000005', 'APPLICANT', 'Job applicant (self-signup)'),
    ('00000000-0000-0000-0000-000000000006', 'MANAGER', 'Line manager of direct reports'),
    ('00000000-0000-0000-0000-000000000007', 'INTERVIEWER', 'Assigned interviewer');

-- LEAVE_ALLOCATE: ADMIN, HR_MANAGER, HR_OFFICER
INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000068'),
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000068'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000068');

-- APPLICANT: job board, own applications, own notifications. Nothing else.
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000005', id
FROM permissions
WHERE name IN (
    'JOB_BOARD_READ',
    'APPLICATION_SELF',
    'NOTIFICATION_READ',
    'NOTIFICATION_MARK_READ',
    'NOTIFICATION_MARK_UNREAD'
);

-- MANAGER: held alongside EMPLOYEE; adds team visibility and approvals,
-- which the controllers scope to direct reports only.
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000006', id
FROM permissions
WHERE name IN (
    'TEAM_READ',
    'EMPLOYEE_READ',
    'LEAVE_APPROVE',
    'LEAVE_REJECT',
    'TIMESHEET_APPROVE',
    'TIMESHEET_REJECT',
    'EXPENSE_APPROVE',
    'RECOGNITION_GIVE'
);

-- INTERVIEWER: own assigned interviews and feedback only.
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000007', id
FROM permissions
WHERE name = 'RECRUITMENT_INTERVIEWER';

-- EMPLOYEE: self-service only. No staff list, directory, departments,
-- interviews, or giving recognition (which needs a colleague picker).
DELETE FROM role_permissions
WHERE role_id = '00000000-0000-0000-0000-000000000004'
  AND permission_id IN (
      SELECT id FROM permissions
      WHERE name IN (
          'EMPLOYEE_READ',
          'DEPARTMENT_READ',
          'DIRECTORY_READ',
          'RECRUITMENT_INTERVIEWER',
          'RECOGNITION_GIVE'
      )
  );

-- ============================================================
-- EXISTING ACCOUNTS
-- ============================================================

-- Self-signups that were never linked to an employee record become
-- applicants: add APPLICANT, then drop EMPLOYEE.
INSERT INTO user_roles (user_id, role_id)
SELECT ur.user_id, '00000000-0000-0000-0000-000000000005'
FROM user_roles ur
WHERE ur.role_id = '00000000-0000-0000-0000-000000000004'
  AND NOT EXISTS (SELECT 1 FROM employees e WHERE e.user_id = ur.user_id)
  AND NOT EXISTS (
      SELECT 1 FROM user_roles other
      WHERE other.user_id = ur.user_id
        AND other.role_id IN (
            '00000000-0000-0000-0000-000000000001',
            '00000000-0000-0000-0000-000000000002',
            '00000000-0000-0000-0000-000000000003'
        )
  )
ON CONFLICT DO NOTHING;

DELETE FROM user_roles ur
WHERE ur.role_id = '00000000-0000-0000-0000-000000000004'
  AND EXISTS (
      SELECT 1 FROM user_roles a
      WHERE a.user_id = ur.user_id
        AND a.role_id = '00000000-0000-0000-0000-000000000005'
  );

-- Anyone who is already someone's manager gets MANAGER.
INSERT INTO user_roles (user_id, role_id)
SELECT DISTINCT m.user_id, '00000000-0000-0000-0000-000000000006'
FROM employees m
WHERE EXISTS (SELECT 1 FROM employees e WHERE e.manager_id = m.id)
ON CONFLICT DO NOTHING;

-- Anyone already assigned to an interview gets INTERVIEWER.
INSERT INTO user_roles (user_id, role_id)
SELECT DISTINCT i.interviewer_user_id, '00000000-0000-0000-0000-000000000007'
FROM interviews i
ON CONFLICT DO NOTHING;
