-- certificate_signatory_snapshots was created in V161 with only created_at and is_deleted,
-- but the entity extends BaseEntity, which also declares updated_at, created_by and
-- updated_by. Every read therefore failed with "column ... does not exist", which took
-- down certificate PDF export - the table's only consumer.
--
-- Added here rather than recreating the table so any captured rows survive, and matching
-- the audit-column convention already used by the certificates table.
ALTER TABLE certificate_signatory_snapshots
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE certificate_signatory_snapshots
    ADD COLUMN IF NOT EXISTS created_by VARCHAR;

ALTER TABLE certificate_signatory_snapshots
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR;

COMMENT ON COLUMN certificate_signatory_snapshots.updated_at IS 'Last modification time, from BaseEntity.updatedAt.';
COMMENT ON COLUMN certificate_signatory_snapshots.created_by IS 'Email of the actor who captured the snapshot, from BaseEntity.createdBy.';
COMMENT ON COLUMN certificate_signatory_snapshots.updated_by IS 'Email of the last actor, from BaseEntity.updatedBy.';
