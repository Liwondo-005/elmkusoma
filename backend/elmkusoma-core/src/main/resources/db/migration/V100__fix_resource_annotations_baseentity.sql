-- Fix resource_annotations table to match BaseEntity
-- Add missing BaseEntity columns
ALTER TABLE resource_annotations ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE resource_annotations ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE resource_annotations ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure created_at exists (should already from entity)
ALTER TABLE resource_annotations ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE resource_annotations ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Add institution_id for multi-tenancy
ALTER TABLE resource_annotations ADD COLUMN IF NOT EXISTS institution_id UUID;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_annotations_institution ON resource_annotations(institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_annotations_created_by ON resource_annotations(created_by) WHERE is_deleted = false;
