CREATE TABLE attendance (
                            id UUID PRIMARY KEY,

                            employee_id UUID NOT NULL,

                            attendance_date DATE NOT NULL,

                            clock_in TIMESTAMPTZ,
                            clock_out TIMESTAMPTZ,

                            status VARCHAR(30) NOT NULL DEFAULT 'PRESENT',

                            notes VARCHAR(500),

                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_attendance_employee
                                FOREIGN KEY (employee_id)
                                    REFERENCES employees(id)
                                    ON DELETE RESTRICT,

                            CONSTRAINT uk_attendance_employee_date
                                UNIQUE (employee_id, attendance_date),

                            CONSTRAINT chk_attendance_status
                                CHECK (
                                    status IN (
                                               'PRESENT',
                                               'ABSENT',
                                               'LATE',
                                               'HALF_DAY',
                                               'ON_LEAVE',
                                               'REMOTE'
                                        )
                                    ),

                            CONSTRAINT chk_attendance_clock_times
                                CHECK (
                                    clock_out IS NULL
                                        OR clock_in IS NULL
                                        OR clock_out >= clock_in
                                    )
);