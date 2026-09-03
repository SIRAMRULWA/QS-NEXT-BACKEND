-- ============================================================
-- RBAC SEED DATA
-- ============================================================

-- ============================================================
-- ROLES
-- ============================================================

INSERT INTO roles (id, name, description)
VALUES
    ('00000000-0000-0000-0000-000000000001',
     'ADMIN',
     'Full system administrator'),

    ('00000000-0000-0000-0000-000000000002',
     'HR_MANAGER',
     'Human resources manager'),

    ('00000000-0000-0000-0000-000000000003',
     'HR_OFFICER',
     'Human resources officer'),

    ('00000000-0000-0000-0000-000000000004',
     'EMPLOYEE',
     'Standard employee');


-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT INTO permissions (id, name, description)
VALUES

    -- Employee
    ('10000000-0000-0000-0000-000000000001',
     'EMPLOYEE_READ',
     'View employee information'),

    ('10000000-0000-0000-0000-000000000002',
     'EMPLOYEE_CREATE',
     'Create employees'),

    ('10000000-0000-0000-0000-000000000003',
     'EMPLOYEE_UPDATE',
     'Update employee information'),

    ('10000000-0000-0000-0000-000000000004',
     'EMPLOYEE_STATUS_UPDATE',
     'Update employee employment status'),


    -- Department
    ('10000000-0000-0000-0000-000000000005',
     'DEPARTMENT_READ',
     'View departments'),

    ('10000000-0000-0000-0000-000000000006',
     'DEPARTMENT_CREATE',
     'Create departments'),

    ('10000000-0000-0000-0000-000000000007',
     'DEPARTMENT_UPDATE',
     'Update departments'),

    ('10000000-0000-0000-0000-000000000008',
     'DEPARTMENT_DELETE',
     'Delete departments'),


    -- Leave
    ('10000000-0000-0000-0000-000000000009',
     'LEAVE_READ',
     'View leave requests'),

    ('10000000-0000-0000-0000-000000000010',
     'LEAVE_CREATE',
     'Create leave requests'),

    ('10000000-0000-0000-0000-000000000011',
     'LEAVE_APPROVE',
     'Approve leave requests'),

    ('10000000-0000-0000-0000-000000000012',
     'LEAVE_REJECT',
     'Reject leave requests'),

    ('10000000-0000-0000-0000-000000000013',
     'LEAVE_CANCEL',
     'Cancel leave requests'),


    -- Attendance
    ('10000000-0000-0000-0000-000000000014',
     'ATTENDANCE_READ',
     'View attendance records'),

    ('10000000-0000-0000-0000-000000000015',
     'ATTENDANCE_CREATE',
     'Create attendance records'),

    ('10000000-0000-0000-0000-000000000016',
     'ATTENDANCE_CLOCK_IN',
     'Clock employees in'),

    ('10000000-0000-0000-0000-000000000017',
     'ATTENDANCE_CLOCK_OUT',
     'Clock employees out'),

    ('10000000-0000-0000-0000-000000000018',
     'ATTENDANCE_MARK_ABSENT',
     'Mark employees absent'),

    ('10000000-0000-0000-0000-000000000019',
     'ATTENDANCE_MARK_LATE',
     'Mark employees late'),

    ('10000000-0000-0000-0000-000000000020',
     'ATTENDANCE_MARK_REMOTE',
     'Mark employees as remote'),


    -- Timesheet
    ('10000000-0000-0000-0000-000000000021',
     'TIMESHEET_READ',
     'View timesheets'),

    ('10000000-0000-0000-0000-000000000022',
     'TIMESHEET_CREATE',
     'Create timesheets'),

    ('10000000-0000-0000-0000-000000000023',
     'TIMESHEET_ENTRY_CREATE',
     'Add timesheet entries'),

    ('10000000-0000-0000-0000-000000000024',
     'TIMESHEET_SUBMIT',
     'Submit timesheets'),

    ('10000000-0000-0000-0000-000000000025',
     'TIMESHEET_APPROVE',
     'Approve timesheets'),

    ('10000000-0000-0000-0000-000000000026',
     'TIMESHEET_REJECT',
     'Reject timesheets'),

    ('10000000-0000-0000-0000-000000000027',
     'TIMESHEET_ENTRY_DELETE',
     'Delete timesheet entries'),


    -- Notifications
    ('10000000-0000-0000-0000-000000000028',
     'NOTIFICATION_READ',
     'View notifications'),

    ('10000000-0000-0000-0000-000000000029',
     'NOTIFICATION_MARK_READ',
     'Mark notifications as read'),

    ('10000000-0000-0000-0000-000000000030',
     'NOTIFICATION_MARK_UNREAD',
     'Mark notifications as unread'),


    -- Users
    ('10000000-0000-0000-0000-000000000031',
     'USER_READ',
     'View users'),

    ('10000000-0000-0000-0000-000000000032',
     'USER_DISABLE',
     'Disable user accounts'),

    ('10000000-0000-0000-0000-000000000033',
     'USER_ENABLE',
     'Enable user accounts'),


    -- Reports
    ('10000000-0000-0000-0000-000000000034',
     'REPORT_READ',
     'View reports');


-- ============================================================
-- ADMIN
-- Full access
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    '00000000-0000-0000-0000-000000000001',
    id
FROM permissions;


-- ============================================================
-- HR_MANAGER
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    '00000000-0000-0000-0000-000000000002',
    id
FROM permissions
WHERE name IN (

               'EMPLOYEE_READ',
               'EMPLOYEE_CREATE',
               'EMPLOYEE_UPDATE',
               'EMPLOYEE_STATUS_UPDATE',

               'DEPARTMENT_READ',
               'DEPARTMENT_CREATE',
               'DEPARTMENT_UPDATE',

               'LEAVE_READ',
               'LEAVE_CREATE',
               'LEAVE_APPROVE',
               'LEAVE_REJECT',
               'LEAVE_CANCEL',

               'ATTENDANCE_READ',
               'ATTENDANCE_CREATE',
               'ATTENDANCE_CLOCK_IN',
               'ATTENDANCE_CLOCK_OUT',
               'ATTENDANCE_MARK_ABSENT',
               'ATTENDANCE_MARK_LATE',
               'ATTENDANCE_MARK_REMOTE',

               'TIMESHEET_READ',
               'TIMESHEET_CREATE',
               'TIMESHEET_ENTRY_CREATE',
               'TIMESHEET_SUBMIT',
               'TIMESHEET_APPROVE',
               'TIMESHEET_REJECT',
               'TIMESHEET_ENTRY_DELETE',

               'NOTIFICATION_READ',
               'NOTIFICATION_MARK_READ',
               'NOTIFICATION_MARK_UNREAD',

               'USER_READ',
               'REPORT_READ'
    );


-- ============================================================
-- HR_OFFICER
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    '00000000-0000-0000-0000-000000000003',
    id
FROM permissions
WHERE name IN (

               'EMPLOYEE_READ',
               'EMPLOYEE_CREATE',
               'EMPLOYEE_UPDATE',

               'DEPARTMENT_READ',

               'LEAVE_READ',
               'LEAVE_CREATE',

               'ATTENDANCE_READ',
               'ATTENDANCE_CREATE',
               'ATTENDANCE_CLOCK_IN',
               'ATTENDANCE_CLOCK_OUT',

               'TIMESHEET_READ',
               'TIMESHEET_CREATE',
               'TIMESHEET_ENTRY_CREATE',
               'TIMESHEET_SUBMIT',

               'NOTIFICATION_READ',
               'NOTIFICATION_MARK_READ',
               'NOTIFICATION_MARK_UNREAD',

               'USER_READ',
               'REPORT_READ'
    );


-- ============================================================
-- EMPLOYEE
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    '00000000-0000-0000-0000-000000000004',
    id
FROM permissions
WHERE name IN (

               'EMPLOYEE_READ',

               'DEPARTMENT_READ',

               'LEAVE_READ',
               'LEAVE_CREATE',
               'LEAVE_CANCEL',

               'ATTENDANCE_READ',
               'ATTENDANCE_CLOCK_IN',
               'ATTENDANCE_CLOCK_OUT',

               'TIMESHEET_READ',
               'TIMESHEET_CREATE',
               'TIMESHEET_ENTRY_CREATE',
               'TIMESHEET_SUBMIT',

               'NOTIFICATION_READ',
               'NOTIFICATION_MARK_READ',
               'NOTIFICATION_MARK_UNREAD'
    );