-- V132: Per-provider ownership for provider roles (audit B-14, X-7)
--
-- Provider identity was unrepresentable in the scoping table: institution_memberships.role only
-- knows OWNER/ADMIN/TEACHER/..., so a PROVIDER_ADMIN was stored as a plain ADMIN and could never
-- be told apart from an institution admin. The purpose-built guard OwnershipGuard.verifyProvider
-- existed but had no caller, and the provider_memberships table created in V63 was never wired.
--
-- Rather than introduce a second membership system, this adds an optional provider_id to the
-- EXISTING membership row. A membership with provider_id set means "this account administers
-- exactly this provider". Memberships without it keep institution-wide scope, so every existing
-- account behaves exactly as before.
--
-- Additive only; no row is deleted and no existing column changes meaning.

ALTER TABLE institution_memberships
    ADD COLUMN IF NOT EXISTS provider_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_memberships_provider' AND conrelid = 'institution_memberships'::regclass) THEN
        ALTER TABLE institution_memberships
            ADD CONSTRAINT fk_memberships_provider
            FOREIGN KEY (provider_id) REFERENCES nfe_education_providers(id) NOT VALID;
    END IF;
END $$;

-- Neutralise memberships whose provider no longer exists so the FK can be validated.
UPDATE institution_memberships m
   SET provider_id = NULL, updated_at = NOW()
 WHERE m.provider_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM nfe_education_providers p WHERE p.id = m.provider_id);

DO $$
BEGIN
    ALTER TABLE institution_memberships VALIDATE CONSTRAINT fk_memberships_provider;
EXCEPTION
    WHEN others THEN
        RAISE NOTICE 'V132: leaving fk_memberships_provider NOT VALID (%)', SQLERRM;
END $$;

CREATE INDEX IF NOT EXISTS idx_institution_memberships_provider
    ON institution_memberships (provider_id) WHERE is_deleted = false AND provider_id IS NOT NULL;

-- Give the provider accounts that already exist a real provider scope, so the new ownership rule
-- is effective immediately instead of only for future accounts. One membership per provider-org
-- account; accounts with no provider row yet keep institution-wide scope (unchanged behaviour).
UPDATE institution_memberships m
   SET provider_id = p.id, updated_at = NOW()
  FROM users u, nfe_education_providers p
 WHERE m.user_id = u.id
   AND m.provider_id IS NULL
   AND u.role IN ('PROVIDER_ADMIN', 'PROVIDER_STAFF')
   AND u.is_deleted = false
   AND p.institution_id = m.institution_id
   AND p.is_deleted = false
   AND NOT EXISTS (SELECT 1 FROM nfe_education_providers p2
                    WHERE p2.institution_id = m.institution_id AND p2.is_deleted = false
                      AND p2.id <> p.id);