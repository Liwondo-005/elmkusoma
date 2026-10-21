-- AU-03 (Payment.md section 58/71): an audit row must be traceable back to the HTTP
-- request that produced it, so a support conversation can be joined to the exact line
-- in the application log instead of to a two-second window.
--
-- The value comes from the MDC correlation id stamped by RequestCorrelationFilter, which
-- generates a UUID when a caller supplies no X-Correlation-Id and discards any inbound
-- value that does not match [A-Za-z0-9._-]{1,64}. AuditLog therefore cannot receive a
-- longer string than the column holds.
--
-- Nullable on purpose. Schedulers, migrations, imports and webhook retries all write audit
-- rows outside any request thread and legitimately have no correlation id; NULL means "no
-- request", which is a true statement and not a missing value. It must never be backfilled
-- or defaulted to something invented.
--
-- Additive only: no existing column is dropped, narrowed or rewritten, so applying it over
-- live audit history cannot lose a row.
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(64);

-- Operators look up by correlation id; an unindexed column would be a sequential scan over
-- the full audit table. Partial, because most rows written by background jobs are NULL.
CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation_id
    ON audit_logs (correlation_id)
    WHERE correlation_id IS NOT NULL;
