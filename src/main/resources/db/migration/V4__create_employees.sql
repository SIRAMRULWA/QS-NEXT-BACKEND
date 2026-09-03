CREATE TABLE employees (
                           id UUID PRIMARY KEY,

                           user_id UUID NOT NULL,
                           department_id UUID NOT NULL,

                           employee_number VARCHAR(50) NOT NULL,

                           first_name VARCHAR(100) NOT NULL,
                           last_name VARCHAR(100) NOT NULL,

                           phone_number VARCHAR(30),
                           job_title VARCHAR(150),

                           hire_date DATE NOT NULL,

                           employment_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT uk_employees_user_id UNIQUE (user_id),
                           CONSTRAINT uk_employees_employee_number UNIQUE (employee_number),

                           CONSTRAINT fk_employees_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users(id)
                                   ON DELETE RESTRICT,

                           CONSTRAINT fk_employees_department
                               FOREIGN KEY (department_id)
                                   REFERENCES departments(id)
                                   ON DELETE RESTRICT,

                           CONSTRAINT chk_employees_status
                               CHECK (
                                   employment_status IN (
                                                         'ACTIVE',
                                                         'INACTIVE',
                                                         'SUSPENDED',
                                                         'TERMINATED'
                                       )
                                   )
);