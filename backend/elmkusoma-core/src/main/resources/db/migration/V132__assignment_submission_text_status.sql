-- B16: learner submission workflow on assignment_submissions
-- (submission body, explicit status, draft flag).
-- Idempotent: V110 may already have added some of these columns.
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS submission_text TEXT;
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'SUBMITTED';
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS is_draft BOOLEAN DEFAULT FALSE;

ALTER TABLE assignment_submissions ALTER COLUMN status TYPE VARCHAR(32);

-- Existing rows were submitted implicitly before the status column existed.
UPDATE assignment_submissions SET status = 'SUBMITTED' WHERE status IS NULL;
