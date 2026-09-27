-- Fix resource_visibility enum columns to VARCHAR in resources and video_tutorials tables
-- to match @Enumerated(EnumType.STRING) in entities

-- Fix resources.visibility
ALTER TABLE resources ALTER COLUMN visibility DROP DEFAULT;
ALTER TABLE resources ALTER COLUMN visibility TYPE VARCHAR(20) USING visibility::VARCHAR;
ALTER TABLE resources ALTER COLUMN visibility SET DEFAULT 'DRAFT';

-- Fix video_tutorials.visibility
ALTER TABLE video_tutorials ALTER COLUMN visibility DROP DEFAULT;
ALTER TABLE video_tutorials ALTER COLUMN visibility TYPE VARCHAR(20) USING visibility::VARCHAR;
ALTER TABLE video_tutorials ALTER COLUMN visibility SET DEFAULT 'DRAFT';

-- Drop the custom enum type
DROP TYPE IF EXISTS resource_visibility;
