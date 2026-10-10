-- E2E fixtures for the provider workspace specs.
--
-- Idempotent: safe to re-run. Provisioned here rather than in application code because these
-- accounts are for the Playwright suite only and must never be creatable by a running server.
--
-- provider-workspace.spec.ts expects exactly these constants:
--   PROVIDER_EMAIL     = provider-admin@arushatc-e2e.test
--   PROVIDER_PASSWORD  = password
--   FOREIGN_INSTITUTION = 11111111-1111-1111-1111-111111111111   (provider has no membership)
--
-- Both are named constants in the spec, so nothing here is guessed: the spec states the
-- identifiers it will use and this creates them.
--
-- Password is "password". The hash below is the one this database already uses for its
-- development fixtures, so it is known to authenticate rather than merely plausible.

-- Foreign institution the provider admin must NOT have access to.
INSERT INTO institutions (id, created_at, is_deleted, name, code, type, country, is_active, status)
VALUES ('11111111-1111-1111-1111-111111111111', NOW(), false,
        'E2E Foreign Institution', 'E2E-FOREIGN', 'SECONDARY', 'Tanzania', true, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- Provider admin account.
INSERT INTO users (id, institution_id, created_at, is_deleted, email, password_hash,
                   first_name, last_name, role, is_active, is_email_verified)
VALUES (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000003', NOW(), false,
        'provider-admin@arushatc-e2e.test',
        '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2',
        'E2E', 'Provider Admin', 'PROVIDER_ADMIN', true, true)
ON CONFLICT DO NOTHING;

-- Membership in the Arusha institution (the provider's own tenant).
INSERT INTO institution_memberships (created_at, is_deleted, user_id, institution_id, role, is_active)
SELECT NOW(), false, u.id, u.institution_id, 'ADMIN', true
FROM users u
WHERE u.email = 'provider-admin@arushatc-e2e.test'
  AND NOT EXISTS (
    SELECT 1 FROM institution_memberships m
    WHERE m.user_id = u.id AND m.institution_id = u.institution_id
  );

-- Teacher record backing the PROVIDER_ADMIN user_id (teachers.id is distinct from users.id).
INSERT INTO teachers (id, institution_id, created_at, is_deleted, user_id, status)
SELECT gen_random_uuid(), u.institution_id, NOW(), false, u.id, 'ACTIVE'
FROM users u
WHERE u.email = 'provider-admin@arushatc-e2e.test'
  AND NOT EXISTS (SELECT 1 FROM teachers t WHERE t.user_id = u.id);

-- The provider record the workspace reads.
INSERT INTO nfe_education_providers (id, institution_id, created_at, is_deleted, name,
                                     provider_type, is_active, is_verified)
SELECT gen_random_uuid(), u.institution_id, NOW(), false, 'E2E Provider Workspace',
       'ORGANIZATION', true, true
FROM users u
WHERE u.email = 'provider-admin@arushatc-e2e.test'
  AND NOT EXISTS (
    SELECT 1 FROM nfe_education_providers p
    WHERE p.institution_id = u.institution_id AND p.is_deleted = false
  );