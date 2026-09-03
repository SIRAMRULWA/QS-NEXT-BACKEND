CREATE TABLE leave_requests (
                                id UUID PRIMARY KEY,

                                employee_id UUID NOT NULL,

                                leave_type VARCHAR(30) NOT NULL,
                                status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                                start_date DATE NOT NULL,
                                end_date DATE NOT NULL,

                                reason VARCHAR(500),

                                approved_by UUID,
                                approved_at TIMESTAMPTZ,

                                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_leave_requests_employee
                                    FOREIGN KEY (employee_id)
                                        REFERENCES employees(id)
                                        ON DELETE RESTRICT,

                                CONSTRAINT fk_leave_requests_approver
                                    FOREIGN KEY (approved_by)
                                        REFERENCES users(id)
                                        ON DELETE SET NULL,

                                CONSTRAINT chk_leave_requests_type
                                    CHECK (
                                        leave_type IN (
                                                       'ANNUAL',
                                                       'SICK',
                                                       'FAMILY_RESPONSIBILITY',
                                                       'MATERNITY',
                                                       'PATERNITY',
                                                       'UNPAID',
                                                       'STUDY',
                                                       'OTHER'
                                            )
                                        ),

                                CONSTRAINT chk_leave_requests_status
                                    CHECK (
                                        status IN (
                                                   'PENDING',
                                                   'APPROVED',
                                                   'REJECTED',
                                                   'CANCELLED'
                                            )
                                        ),

                                CONSTRAINT chk_leave_requests_dates
                                    CHECK (end_date >= start_date)
);


CREATE TABLE leave_balances (
                                id UUID PRIMARY KEY,

                                employee_id UUID NOT NULL,
                                leave_type VARCHAR(30) NOT NULL,

                                leave_year INTEGER NOT NULL,

                                allocated_days NUMERIC(6,2) NOT NULL DEFAULT 0,
                                used_days NUMERIC(6,2) NOT NULL DEFAULT 0,

                                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_leave_balances_employee
                                    FOREIGN KEY (employee_id)
                                        REFERENCES employees(id)
                                        ON DELETE RESTRICT,

                                CONSTRAINT uk_leave_balances_employee_type_year
                                    UNIQUE (employee_id, leave_type, leave_year),

                                CONSTRAINT chk_leave_balances_type
                                    CHECK (
                                        leave_type IN (
                                                       'ANNUAL',
                                                       'SICK',
                                                       'FAMILY_RESPONSIBILITY',
                                                       'MATERNITY',
                                                       'PATERNITY',
                                                       'UNPAID',
                                                       'STUDY',
                                                       'OTHER'
                                            )
                                        ),

                                CONSTRAINT chk_leave_balances_year
                                    CHECK (leave_year >= 2000),

                                CONSTRAINT chk_leave_balances_allocated
                                    CHECK (allocated_days >= 0),

                                CONSTRAINT chk_leave_balances_used
                                    CHECK (used_days >= 0)
);