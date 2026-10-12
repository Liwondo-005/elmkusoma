-- Provider staff provisioning scope (authority levels C -> D)
--
-- V146 gave provider roles a provider_id on the membership row, which made per-provider
-- ownership representable. Two gaps remained, and both blocked a Provider Owner from doing
-- anything the product promises they can do:
--
--   1. An invitation could not name a provider. InstitutionPeopleService.resolveProviderScopeForInvitation
--      picked the institution's provider with findFirst(), so with more than one live provider
--      per institution the redeemed staff account could land on the wrong one - silently, and
--      only sometimes. This adds institution_invitations.provider_id so the scope an inviter
--      chose is the scope that is granted, recorded at issue time rather than inferred later.
--
--   2. Staff had no responsibility. Provider staff are all one flat role, so a records clerk
--      and a trainer receive identical authority. This adds a free-form
--      institution_memberships.responsibility label. It is deliberately NOT a role and NOT
--      permission-bearing: the specification treats job responsibilities as descriptive, and
--      authority still comes from role + provider scope + ownership + lifecycle. Creating a
--      hard-coded role per job title would have widened the authorization model without
--      widening the platform's actual capability.
--
-- Additive only: no row is deleted, no existing column changes meaning, every new column is
-- nullable, and both indexes are partial so unstaffed providers cost nothing.

ALTER TABLE institution_invitations
    ADD COLUMN IF NOT EXISTS provider_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_invitations_provider' AND conrelid = 'institution_invitations'::regclass) THEN
        ALTER TABLE institution_invitations
            ADD CONSTRAINT fk_invitations_provider
            FOREIGN KEY (provider_id) REFERENCES nfe_education_providers(id) NOT VALID;
    END IF;
END $$;

-- Invitations issued before this migration, and invitations for a provider that has since been
-- deleted, keep a NULL provider_id. Redemption already handles NULL by falling back to the
-- institution-wide scope it always used, so these rows stay redeemable exactly as they were.
UPDATE institution_invitations i
   SET provider_id = NULL
 WHERE i.provider_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM nfe_education_providers p WHERE p.id = i.provider_id);

DO $$
BEGIN
    ALTER TABLE institution_invitations VALIDATE CONSTRAINT fk_invitations_provider;
EXCEPTION
    WHEN others THEN
        RAISE NOTICE 'V161: leaving fk_invitations_provider NOT VALID (%)', SQLERRM;
END $$;

CREATE INDEX IF NOT EXISTS idx_invitations_provider
    ON institution_invitations (provider_id) WHERE provider_id IS NOT NULL AND is_deleted = false;

-- Authority level D: the responsibility a staff member carries inside their provider.
-- Free-form text rather than an enum or a lookup table: the set of responsibilities is a
-- business list that changes with what providers are enabled, and storing it as text keeps
-- existing rows valid and needs no migration each time a responsibility is renamed.
ALTER TABLE institution_memberships
    ADD COLUMN IF NOT EXISTS responsibility VARCHAR(60);

-- Existing provider memberships keep a NULL responsibility, which the API surfaces as
-- "UNASSIGNED" rather than inventing a value nobody set.
