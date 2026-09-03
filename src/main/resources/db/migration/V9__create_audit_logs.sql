CREATE TABLE audit_logs (
                            id UUID PRIMARY KEY,

                            user_id UUID,

                            action VARCHAR(100) NOT NULL,

                            entity_type VARCHAR(100) NOT NULL,
                            entity_id UUID,

                            old_values JSONB,
                            new_values JSONB,

                            ip_address INET,
                            user_agent VARCHAR(1000),

                            result VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',

                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_audit_logs_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE SET NULL,

                            CONSTRAINT chk_audit_logs_result
                                CHECK (
                                    result IN (
                                               'SUCCESS',
                                               'FAILURE'
                                        )
                                    )
);