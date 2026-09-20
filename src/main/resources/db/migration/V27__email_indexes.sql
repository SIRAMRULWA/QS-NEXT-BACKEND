-- The emails table (V14) was created with no indexes at all. EmailRepository's
-- outbox-poller queries (findByStatus, findByStatusAndCreatedAtBefore - the
-- outbox sweep's own safety-net query) would sequentially scan the whole
-- table on every poll once it grows past a trivial size.
CREATE INDEX idx_emails_status ON emails (status);

CREATE INDEX idx_emails_status_created_at ON emails (status, created_at);
