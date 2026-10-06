-- V131: assessment attempt limit + status maintenance (B24)

-- max_attempts is carried by the Assessment entity and AssessmentRequest; keep
-- the column guaranteed even on databases that skipped V110.
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS max_attempts INTEGER;

-- status is written by the create/update path but rows created before it was
-- maintained are still NULL.
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS status VARCHAR(30);

UPDATE assessments
   SET status = CASE WHEN is_published THEN 'PUBLISHED' ELSE 'DRAFT' END
 WHERE status IS NULL;
