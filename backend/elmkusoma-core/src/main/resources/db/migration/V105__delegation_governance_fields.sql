-- V90: Delegation governance fields (additive only, no data loss).
-- Extends admin_delegations with authority, reason/notes, scoped resources,
-- and approval tracking. Existing rows default to legacy-compatible values.

ALTER TABLE admin_delegations
    ADD COLUMN IF NOT EXISTS authority VARCHAR(60) NOT NULL DEFAULT 'GENERAL_ADMIN';

ALTER TABLE admin_delegations
    ADD COLUMN IF NOT EXISTS reason TEXT;

ALTER TABLE admin_delegations
    ADD COLUMN IF NOT EXISTS notes TEXT;

ALTER TABLE admin_delegations
    ADD COLUMN IF NOT EXISTS resource_ids JSONB;

ALTER TABLE admin_delegations
    ADD COLUMN IF NOT EXISTS approved_by UUID;

ALTER TABLE admin_delegations
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;

ALTER TABLE admin_delegations
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP;

ALTER TABLE admin_delegations
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_admin_delegations_authority ON admin_delegations(authority);
CREATE INDEX IF NOT EXISTS idx_admin_delegations_delegator ON admin_delegations(delegator_id);
