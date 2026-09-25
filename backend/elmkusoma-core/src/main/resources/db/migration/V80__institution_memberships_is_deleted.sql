-- V80: Entity/schema drift repairs.
-- BaseEntity maps institution_id, created_at, updated_at, created_by, updated_by,
-- is_deleted, but several tables created by earlier migrations lack some of them.
-- Hibernate then generates SELECTs with columns that do not exist (SQLState 42703),
-- breaking membership checks (login/registration), platform config policy, token
-- revocation, and audit/activity reads.

-- institution_memberships: standalone entity maps is_deleted
ALTER TABLE institution_memberships ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- platform_config: V63 omitted BaseEntity.institution_id (see V79)
-- (kept here only as a comment; V79 owns that change)

-- BaseEntity columns for tables whose entities extend BaseEntity
DO $$
DECLARE
  t TEXT;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'institution_activity',
    'institution_audit_log',
    'learning_collaborations',
    'learning_modules',
    'professional_development_goals',
    'replay_progress',
    'revoked_tokens',
    'role_permissions',
    'workshop_sessions',
    'deep_learning_contents'
  ] LOOP
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name=t) THEN
      EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS institution_id UUID', t);
      EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW()', t);
      EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP', t);
      EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS created_by VARCHAR(255)', t);
      EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255)', t);
      EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE', t);
    END IF;
  END LOOP;
END $$;
