-- Add institution_id to video_tutorial_progress table for multi-tenancy
ALTER TABLE video_tutorial_progress ADD COLUMN IF NOT EXISTS institution_id UUID;

-- Create index for institution_id
CREATE INDEX IF NOT EXISTS idx_video_progress_institution ON video_tutorial_progress(institution_id) WHERE is_deleted = false;

-- Backfill institution_id from video_tutorials
UPDATE video_tutorial_progress vtp
SET institution_id = vt.institution_id
FROM video_tutorials vt
WHERE vtp.video_tutorial_id = vt.id
AND vtp.institution_id IS NULL;
