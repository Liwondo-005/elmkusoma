-- V82: Add department_id and campus_id to institution_memberships for scope-based access control

ALTER TABLE institution_memberships ADD COLUMN IF NOT EXISTS department_id UUID;
ALTER TABLE institution_memberships ADD COLUMN IF NOT EXISTS campus_id UUID;

CREATE INDEX IF NOT EXISTS idx_institution_memberships_department ON institution_memberships(department_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_institution_memberships_campus ON institution_memberships(campus_id) WHERE is_deleted = false;