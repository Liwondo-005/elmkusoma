-- Lesson lifecycle column (DRAFT | READY | PUBLISHED | ARCHIVED).
-- Originally introduced as V105; restored under a free version after a migration-number collision.
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

-- Backfill from the legacy boolean
UPDATE lessons SET status = 'PUBLISHED' WHERE is_published = true AND status = 'DRAFT';

CREATE INDEX IF NOT EXISTS idx_lessons_status ON lessons(status);
