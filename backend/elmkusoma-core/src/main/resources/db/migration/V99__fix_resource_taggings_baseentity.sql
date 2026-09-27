-- Fix resource_taggings table to match BaseEntity (UUID id + BaseEntity columns)
-- Drop old primary key and id column
ALTER TABLE resource_taggings DROP CONSTRAINT IF EXISTS resource_taggings_pkey;
ALTER TABLE resource_taggings DROP COLUMN IF EXISTS id CASCADE;
ALTER TABLE resource_taggings ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE resource_taggings ADD PRIMARY KEY (id);

-- Change tag_id from BIGINT to UUID
ALTER TABLE resource_taggings ALTER COLUMN tag_id TYPE UUID USING tag_id::UUID;

-- Add BaseEntity columns
ALTER TABLE resource_taggings ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE resource_taggings ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE resource_taggings ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE resource_taggings ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE resource_taggings ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Add institution_id for multi-tenancy
ALTER TABLE resource_taggings ADD COLUMN IF NOT EXISTS institution_id UUID;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_resource_taggings_institution ON resource_taggings(institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_resource_taggings_created_by ON resource_taggings(created_by) WHERE is_deleted = false;
