-- institution_memberships: the entity declares is_deleted (soft-delete contract)
-- but the table was created without it, so every INSERT from
-- AuthServiceImpl.ensureMembership failed with SQLState 42703 (registration 500).
ALTER TABLE institution_memberships
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;