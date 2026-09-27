-- Fix video_tutorials created_by column type from UUID to VARCHAR to match BaseEntity
ALTER TABLE video_tutorials ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::VARCHAR;

-- Also fix updated_by to be consistent (already VARCHAR but ensure)
-- No change needed for updated_by as it's already VARCHAR(255)
