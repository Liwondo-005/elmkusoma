-- V74: add events.access_level
-- The Event entity (tz.elmkusoma.event.domain.Event) maps access_level VARCHAR(30),
-- but V24 never created the column, so authenticated event queries failed with:
--   column e1_0.access_level does not exist  -> HTTP 500
-- Flyway is skipped at startup (FlywayConfig) so this DDL was applied manually;
-- the file is recorded here for migration history. Idempotent on purpose.
ALTER TABLE events ADD COLUMN IF NOT EXISTS access_level VARCHAR(30);
