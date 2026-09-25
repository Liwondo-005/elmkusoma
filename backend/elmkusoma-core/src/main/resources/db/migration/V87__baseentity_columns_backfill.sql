-- V86: Universal BaseEntity column backfill.
-- 160+ entities extend BaseEntity (institution_id, created_at, updated_at,
-- created_by, updated_by, is_deleted) but many tables were created before those
-- fields existed. Hibernate SELECTs all mapped columns regardless of @DynamicInsert,
-- so any missing column breaks reads with SQLState 42703 (seen on
-- live_class_attendance_detail.created_by, institution_memberships.is_deleted,
-- platform_config.institution_id, revoked_tokens.*, role_permissions.*).
-- Extra DB columns not referenced by an entity are ignored by Hibernate, so this
-- backfill is safe for every table.

DO $$
DECLARE
  t TEXT;
BEGIN
  FOR t IN
    SELECT table_name FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_type = 'BASE TABLE'
      AND table_name <> 'flyway_schema_history'
  LOOP
    EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS institution_id UUID', t);
    EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW()', t);
    EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP', t);
    EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS created_by VARCHAR(255)', t);
    EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255)', t);
    EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE', t);
  END LOOP;
END $$;
