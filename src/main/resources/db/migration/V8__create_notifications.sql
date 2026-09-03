CREATE TABLE notifications (
                               id UUID PRIMARY KEY,

                               user_id UUID NOT NULL,

                               type VARCHAR(50) NOT NULL,
                               title VARCHAR(150) NOT NULL,
                               message VARCHAR(1000) NOT NULL,

                               is_read BOOLEAN NOT NULL DEFAULT FALSE,

                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               read_at TIMESTAMPTZ,

                               CONSTRAINT fk_notifications_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT chk_notifications_read_at
                                   CHECK (
                                       (is_read = FALSE AND read_at IS NULL)
                                           OR
                                       (is_read = TRUE AND read_at IS NOT NULL)
                                       )
);