-- ============================================================
-- OBSERVABILITY
--
-- Adds the correlation ID column the original Audit design (V9)
-- already anticipated ("Request/correlation ID" in its field list) but
-- had no infrastructure to populate yet - see CorrelationIdFilter,
-- which generates or propagates one for every request and puts it in
-- both the MDC (for log lines) and this column (for audit records).
-- Nullable: rows written before this migration have none, and that is
-- expected, not an error.
-- ============================================================

ALTER TABLE audit_logs
    ADD COLUMN correlation_id VARCHAR(100);

CREATE INDEX idx_audit_logs_correlation_id
    ON audit_logs (correlation_id);
