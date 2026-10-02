-- V123: Teacher assignment lifecycle for reassignment history.
-- Additive only: every existing teacher_assignments row becomes an ACTIVE
-- assignment; ENDED/CANCELLED rows are written from now on and never deleted,
-- so historical assignments (and the resources created against them) survive
-- teacher reassignment.

ALTER TABLE teacher_assignments
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE teacher_assignments
    ADD COLUMN IF NOT EXISTS start_date DATE;

ALTER TABLE teacher_assignments
    ADD COLUMN IF NOT EXISTS end_date DATE;

-- Backfill rows written before this migration (the DEFAULT above already
-- covers columns added to a populated table on PostgreSQL 11+).
UPDATE teacher_assignments
   SET status = 'ACTIVE'
 WHERE status IS NULL;

CREATE INDEX IF NOT EXISTS idx_teacher_assignments_status
    ON teacher_assignments (teacher_id, status)
    WHERE is_deleted = false;
