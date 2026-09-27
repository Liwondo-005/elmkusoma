-- Fix resource_tags table to match BaseEntity (UUID id + BaseEntity columns)
-- First drop foreign key from resource_taggings
ALTER TABLE resource_taggings DROP CONSTRAINT IF EXISTS resource_taggings_tag_id_fkey;

-- Now fix resource_tags
ALTER TABLE resource_tags DROP COLUMN IF EXISTS id CASCADE;
ALTER TABLE resource_tags ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE resource_tags ADD PRIMARY KEY (id);

-- Add BaseEntity columns
ALTER TABLE resource_tags ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE resource_tags ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE resource_tags ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE resource_tags ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE resource_tags ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Add institution_id for multi-tenancy
ALTER TABLE resource_tags ADD COLUMN IF NOT EXISTS institution_id UUID;

-- Recreate foreign key with new UUID id
ALTER TABLE resource_taggings ADD CONSTRAINT resource_taggings_tag_id_fkey 
    FOREIGN KEY (tag_id) REFERENCES resource_tags(id) ON DELETE CASCADE;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_resource_tags_institution ON resource_tags(institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_resource_tags_created_by ON resource_tags(created_by) WHERE is_deleted = false;
