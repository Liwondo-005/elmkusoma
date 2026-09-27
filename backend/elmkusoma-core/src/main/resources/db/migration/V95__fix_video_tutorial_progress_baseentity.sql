-- Add missing BaseEntity columns to video_tutorial_progress table
ALTER TABLE video_tutorial_progress ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE video_tutorial_progress ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE video_tutorial_progress ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Add created_by index
CREATE INDEX IF NOT EXISTS idx_video_progress_created_by ON video_tutorial_progress(created_by) WHERE is_deleted = false;
