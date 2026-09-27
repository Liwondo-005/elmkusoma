-- Add institution_id to student_saved_resources table
ALTER TABLE student_saved_resources ADD COLUMN IF NOT EXISTS institution_id UUID;

-- Create index
CREATE INDEX IF NOT EXISTS idx_saved_resources_institution ON student_saved_resources(institution_id) WHERE is_deleted = false;

-- Backfill institution_id from resources
UPDATE student_saved_resources ssr
SET institution_id = r.institution_id
FROM resources r
WHERE ssr.resource_id = r.id
AND ssr.institution_id IS NULL;
