-- V128: Foundation & safety (audit Phase 1)
--
-- 1) B-05  class_group_id foreign keys point at the LEGACY `classes` table while every
--         JPA entity writes `class_groups.id` (academic.domain.ClassGroup @Table("class_groups")).
--         `classes` is a V06 leftover with no entity and no rows. Re-target the three FKs.
-- 2) B-08  institution_memberships (the authorization backbone) has no FKs at all.
-- 3) B-09  Seeded demo accounts ship with a published BCrypt hash in V58/V122.
-- 4)       Missing index on institution_memberships(role) -- hit by countByRoleAndIsDeletedFalse
--         on every platform analytics call.
--
-- All changes are additive/guarded. No historical migration is edited or renumbered.
-- No row is deleted or rewritten except the environment-gated demo-account deactivation.

-- ---------------------------------------------------------------------------
-- 1) B-05: re-target class_group_id FKs from legacy `classes` to `class_groups`
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('public.class_groups') IS NOT NULL THEN
        -- enrollments
        IF EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_enrollments_class_group' AND conrelid = 'enrollments'::regclass) THEN
            ALTER TABLE enrollments DROP CONSTRAINT fk_enrollments_class_group;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conname = 'fk_enrollments_class_group' AND conrelid = 'enrollments'::regclass) THEN
            ALTER TABLE enrollments
                ADD CONSTRAINT fk_enrollments_class_group
                FOREIGN KEY (class_group_id) REFERENCES class_groups(id) NOT VALID;
        END IF;

        -- attendance_records
        IF EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_attendance_class_group' AND conrelid = 'attendance_records'::regclass) THEN
            ALTER TABLE attendance_records DROP CONSTRAINT fk_attendance_class_group;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conname = 'fk_attendance_class_group' AND conrelid = 'attendance_records'::regclass) THEN
            ALTER TABLE attendance_records
                ADD CONSTRAINT fk_attendance_class_group
                FOREIGN KEY (class_group_id) REFERENCES class_groups(id) NOT VALID;
        END IF;

        -- bulk_attendance_sessions
        IF EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_bulk_session_class' AND conrelid = 'bulk_attendance_sessions'::regclass) THEN
            ALTER TABLE bulk_attendance_sessions DROP CONSTRAINT fk_bulk_session_class;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conname = 'fk_bulk_session_class' AND conrelid = 'bulk_attendance_sessions'::regclass) THEN
            ALTER TABLE bulk_attendance_sessions
                ADD CONSTRAINT fk_bulk_session_class
                FOREIGN KEY (class_group_id) REFERENCES class_groups(id) NOT VALID;
        END IF;
    END IF;
END $$;

-- The legacy `classes` table (and its class_schedules/class_teachers children) is left in
-- place: dropping it is destructive and no entity maps it.

-- ---------------------------------------------------------------------------
-- 2) B-08: institution_memberships foreign keys
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_memberships_user' AND conrelid = 'institution_memberships'::regclass) THEN
        ALTER TABLE institution_memberships
            ADD CONSTRAINT fk_memberships_user
            FOREIGN KEY (user_id) REFERENCES users(id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_memberships_institution' AND conrelid = 'institution_memberships'::regclass) THEN
        ALTER TABLE institution_memberships
            ADD CONSTRAINT fk_memberships_institution
            FOREIGN KEY (institution_id) REFERENCES institutions(id) NOT VALID;
    END IF;
END $$;

-- Orphan audit. Every legacy FK row that points outside its new parent is repaired by
-- soft-deleting the orphan (never hard-deleted) so audit history is preserved, then the
-- constraint can be validated.
UPDATE institution_memberships m
   SET is_deleted = true, updated_at = NOW()
 WHERE m.is_deleted = false
   AND (m.user_id NOT IN (SELECT id FROM users)
        OR m.institution_id NOT IN (SELECT id FROM institutions));

UPDATE enrollments e
   SET is_deleted = true, updated_at = NOW()
 WHERE e.is_deleted = false
   AND to_regclass('public.class_groups') IS NOT NULL
   AND e.class_group_id NOT IN (SELECT id FROM class_groups);

UPDATE attendance_records a
   SET is_deleted = true, updated_at = NOW()
 WHERE a.is_deleted = false
   AND to_regclass('public.class_groups') IS NOT NULL
   AND a.class_group_id NOT IN (SELECT id FROM class_groups);

UPDATE bulk_attendance_sessions b
   SET is_deleted = true, updated_at = NOW()
 WHERE b.is_deleted = false
   AND to_regclass('public.class_groups') IS NOT NULL
   AND b.class_group_id NOT IN (SELECT id FROM class_groups);

-- Now that orphans are neutralised, promote the constraints from NOT VALID to validated.
DO $$
DECLARE
    c record;
BEGIN
    FOR c IN
        SELECT conname, conrelid::regclass AS tbl
          FROM pg_constraint
         WHERE conname IN ('fk_enrollments_class_group','fk_attendance_class_group',
                           'fk_bulk_session_class','fk_memberships_user','fk_memberships_institution')
           AND NOT convalidated
    LOOP
        EXECUTE format('ALTER TABLE %s VALIDATE CONSTRAINT %I', c.tbl, c.conname);
    END LOOP;
EXCEPTION
    WHEN others THEN
        -- Never abort the migration: an unvalidated constraint still protects new writes.
        RAISE NOTICE 'V128: could not validate % (%) -- left NOT VALID on purpose', c.conname, SQLERRM;
END $$;

-- ---------------------------------------------------------------------------
-- 3) B-09: seeded demo accounts (V58 / V122 published hashes)
--    Gated by the `demoSeedDeactivate` placeholder:
--      * application.yml       -> false (dev / test / e2e keep working)
--      * application-prod.yml  -> true  (production disables demo logins)
-- ---------------------------------------------------------------------------
UPDATE users
   SET is_active = false, updated_at = NOW()
 WHERE is_deleted = false
   AND is_active = true
   AND ${demoSeedDeactivate}
   AND password_hash IN (
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2'
   );

-- Record the state so an operator can see what happened, without exposing anything.
INSERT INTO platform_config (config_key, config_value, description, is_deleted, created_at, updated_at)
SELECT 'security.demo_seed_accounts_disabled',
       to_char(NOW() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
       'V128 disabled seeded demo accounts holding published password hashes (prod only)',
       false, NOW(), NOW()
 WHERE ${demoSeedDeactivate}
   AND NOT EXISTS (SELECT 1 FROM platform_config
                    WHERE config_key = 'security.demo_seed_accounts_disabled');

-- ---------------------------------------------------------------------------
-- 4) index for the authorization / analytics path on membership role
--    Existing membership indexes are all partial on is_active = true, so they cannot serve
--    countByRoleAndIsDeletedFalse / countByInstitutionIdsAndRoleAndIsDeletedFalse.
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_institution_memberships_role
    ON institution_memberships (role) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_institution_memberships_user_all
    ON institution_memberships (user_id) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_institution_memberships_institution_all
    ON institution_memberships (institution_id) WHERE is_deleted = false;