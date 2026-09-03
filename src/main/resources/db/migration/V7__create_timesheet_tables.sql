CREATE TABLE timesheets (
                            id UUID PRIMARY KEY,

                            employee_id UUID NOT NULL,

                            period_start DATE NOT NULL,
                            period_end DATE NOT NULL,

                            status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

                            submitted_at TIMESTAMPTZ,
                            approved_by UUID,
                            approved_at TIMESTAMPTZ,

                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_timesheets_employee
                                FOREIGN KEY (employee_id)
                                    REFERENCES employees(id)
                                    ON DELETE RESTRICT,

                            CONSTRAINT fk_timesheets_approver
                                FOREIGN KEY (approved_by)
                                    REFERENCES users(id)
                                    ON DELETE SET NULL,

                            CONSTRAINT uk_timesheets_employee_period
                                UNIQUE (employee_id, period_start, period_end),

                            CONSTRAINT chk_timesheets_period
                                CHECK (period_end >= period_start),

                            CONSTRAINT chk_timesheets_status
                                CHECK (
                                    status IN (
                                               'DRAFT',
                                               'SUBMITTED',
                                               'APPROVED',
                                               'REJECTED'
                                        )
                                    )
);


CREATE TABLE timesheet_entries (
                                   id UUID PRIMARY KEY,

                                   timesheet_id UUID NOT NULL,

                                   work_date DATE NOT NULL,

                                   hours_worked NUMERIC(5,2) NOT NULL DEFAULT 0,

                                   description VARCHAR(500),

                                   created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT fk_timesheet_entries_timesheet
                                       FOREIGN KEY (timesheet_id)
                                           REFERENCES timesheets(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT uk_timesheet_entries_date
                                       UNIQUE (timesheet_id, work_date),

                                   CONSTRAINT chk_timesheet_entries_hours
                                       CHECK (
                                           hours_worked >= 0
                                               AND hours_worked <= 24
                                           )
);