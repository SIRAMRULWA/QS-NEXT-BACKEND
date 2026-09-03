-- Employee indexes
CREATE INDEX idx_employees_department_id
    ON employees (department_id);

CREATE INDEX idx_employees_status
    ON employees (employment_status);

CREATE INDEX idx_employees_last_name
    ON employees (last_name);


-- Leave request indexes
CREATE INDEX idx_leave_requests_employee_id
    ON leave_requests (employee_id);

CREATE INDEX idx_leave_requests_status
    ON leave_requests (status);

CREATE INDEX idx_leave_requests_dates
    ON leave_requests (start_date, end_date);


-- Leave balance indexes
CREATE INDEX idx_leave_balances_employee_id
    ON leave_balances (employee_id);


-- Attendance indexes
CREATE INDEX idx_attendance_employee_id
    ON attendance (employee_id);

CREATE INDEX idx_attendance_date
    ON attendance (attendance_date);


-- Timesheet indexes
CREATE INDEX idx_timesheets_employee_id
    ON timesheets (employee_id);

CREATE INDEX idx_timesheets_status
    ON timesheets (status);

CREATE INDEX idx_timesheet_entries_timesheet_id
    ON timesheet_entries (timesheet_id);

CREATE INDEX idx_timesheet_entries_work_date
    ON timesheet_entries (work_date);


-- Notification indexes
CREATE INDEX idx_notifications_user_id
    ON notifications (user_id);

CREATE INDEX idx_notifications_unread
    ON notifications (user_id, is_read);

CREATE INDEX idx_notifications_created_at
    ON notifications (created_at);


-- Audit indexes
CREATE INDEX idx_audit_logs_user_id
    ON audit_logs (user_id);

CREATE INDEX idx_audit_logs_entity
    ON audit_logs (entity_type, entity_id);

CREATE INDEX idx_audit_logs_created_at
    ON audit_logs (created_at);

CREATE INDEX idx_audit_logs_action
    ON audit_logs (action);