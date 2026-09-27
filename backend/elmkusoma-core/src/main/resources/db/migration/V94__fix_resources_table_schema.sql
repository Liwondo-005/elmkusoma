-- Fix resources table: drop old file_url column and fix constraints

-- Drop the NOT NULL constraint on file_url first (if it exists)
-- The column should be dropped since it's been migrated to storage_url
ALTER TABLE resources ALTER COLUMN file_url DROP NOT NULL;

-- Drop the file_url column as it's no longer needed (migrated to storage_url)
ALTER TABLE resources DROP COLUMN IF EXISTS file_url;

-- Drop the file_type column as it's been migrated to resource_type
ALTER TABLE resources DROP COLUMN IF EXISTS file_type;

-- Drop the class_group_id column as it's been migrated to lesson_id
ALTER TABLE resources DROP COLUMN IF EXISTS class_group_id;

-- Drop the subject_id column as it's been migrated to course_id
ALTER TABLE resources DROP COLUMN IF EXISTS subject_id;

-- Drop the is_public column as it's been migrated to visibility
ALTER TABLE resources DROP COLUMN IF EXISTS is_public;

-- Update the check constraint to use the new schema
-- The existing constraint chk_resource_file references storage_url which is correct
-- But we need to ensure it works with the new resource_type enum (now VARCHAR)
ALTER TABLE resources DROP CONSTRAINT IF EXISTS chk_resource_file;
ALTER TABLE resources ADD CONSTRAINT chk_resource_file 
    CHECK (resource_type = 'EXTERNAL_LINK' OR storage_url IS NOT NULL);
