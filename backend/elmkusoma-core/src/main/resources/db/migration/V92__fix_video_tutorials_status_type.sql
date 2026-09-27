-- Fix video_tutorials status column type from custom enum to VARCHAR to match @Enumerated(EnumType.STRING)

-- First remove the default value that depends on the enum type
ALTER TABLE video_tutorials ALTER COLUMN status DROP DEFAULT;

-- Then change the column type
ALTER TABLE video_tutorials ALTER COLUMN status TYPE VARCHAR(20) USING status::VARCHAR;

-- Add back default as string
ALTER TABLE video_tutorials ALTER COLUMN status SET DEFAULT 'DRAFT';

-- Drop the custom enum type
DROP TYPE IF EXISTS video_tutorial_status;
