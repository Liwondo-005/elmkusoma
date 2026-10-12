-- V129: Platform-admin per-account permission matrix (audit B-10)
--
-- `role_permissions.role_id` was carrying TWO different key spaces in one column:
--   * AdministrationService.createRole writes custom_roles.id
--   * PlatformAdminService.listAdmins / the admin permission matrix read+write users.id
-- A UUID collision therefore made an account's matrix clobber a role's permissions.
--
-- This migration introduces a dedicated, user-scoped table and backfills any existing
-- user-keyed rows deterministically. The legacy `/v1/platform-admin/roles/{roleId}/permissions`
-- route keeps working untouched for real roles.
--
-- Additive only: no rows are deleted, the legacy table is left in place.

CREATE TABLE IF NOT EXISTS admin_user_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    permission VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Backfill ONLY rows that unambiguously refer to a user (key matches a users.id and does
-- NOT collide with any custom role id). Ambiguous rows are left for an operator decision.
INSERT INTO admin_user_permissions (user_id, permission, created_at, created_by)
SELECT DISTINCT ON (rp.role_id, rp.permission)
       rp.role_id, rp.permission, rp.created_at, rp.created_by
  FROM role_permissions rp
 WHERE rp.role_id IN (SELECT id FROM users)
   AND rp.role_id NOT IN (SELECT id FROM custom_roles)
   AND to_regclass('public.users') IS NOT NULL
 ORDER BY rp.role_id, rp.permission, rp.created_at;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'admin_user_permissions_pkey' AND conrelid = 'admin_user_permissions'::regclass) THEN
        ALTER TABLE admin_user_permissions ADD CONSTRAINT admin_user_permissions_pkey PRIMARY KEY (id);
    END IF;
END $$;

-- FK added NOT VALID first so existing rows are never rejected, then validated:
-- the backfill above only ever inserts ids that exist in users.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_admin_user_permissions_user' AND conrelid = 'admin_user_permissions'::regclass) THEN
        ALTER TABLE admin_user_permissions
            ADD CONSTRAINT fk_admin_user_permissions_user
            FOREIGN KEY (user_id) REFERENCES users(id) NOT VALID;
    END IF;
END $$;

DELETE FROM admin_user_permissions a
 WHERE a.user_id NOT IN (SELECT id FROM users);

DO $$
BEGIN
    ALTER TABLE admin_user_permissions VALIDATE CONSTRAINT fk_admin_user_permissions_user;
EXCEPTION
    WHEN others THEN
        RAISE NOTICE 'V129: leaving fk_admin_user_permissions_user NOT VALID (%)', SQLERRM;
END $$;

-- Read path (admin list) filters by user and by permission set.
CREATE INDEX IF NOT EXISTS idx_admin_user_permissions_user
    ON admin_user_permissions (user_id) WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS ux_admin_user_permissions_user_permission
    ON admin_user_permissions (user_id, permission) WHERE is_deleted = false;