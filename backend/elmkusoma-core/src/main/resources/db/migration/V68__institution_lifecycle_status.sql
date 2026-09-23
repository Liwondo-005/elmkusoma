-- V68: Institution lifecycle status (ACTIVE -> SUSPENDED -> DEACTIVATED -> ARCHIVED)
-- Per spec: offboarding must not silently destroy data; status tracks lifecycle explicitly.

ALTER TABLE institutions ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_institutions_status ON institutions(status);

UPDATE institutions SET status = 'ACTIVE' WHERE status IS NULL;
