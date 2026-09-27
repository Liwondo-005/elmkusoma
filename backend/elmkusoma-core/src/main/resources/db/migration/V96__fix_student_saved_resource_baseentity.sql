-- Add missing BaseEntity columns to student_saved_resources table
ALTER TABLE student_saved_resources ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE student_saved_resources ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE student_saved_resources ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Rename saved_at to created_at for BaseEntity compatibility
-- (or keep both and handle in entity)
ALTER TABLE student_saved_resources ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Update created_at from saved_at where null
UPDATE student_saved_resources SET created_at = saved_at WHERE created_at IS NULL;

-- Add created_by index
CREATE INDEX IF NOT EXISTS idx_saved_resources_created_by ON student_saved_resources(created_by) WHERE is_deleted = false;
