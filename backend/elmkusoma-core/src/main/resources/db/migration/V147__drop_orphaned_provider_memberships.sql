-- V147: drop the orphaned provider_memberships table (audit B-13)
--
-- The table and its entity were created in V63 but never used: no repository existed, the builder
-- was never called, and nothing read it. Provider scope is now expressed on the EXISTING
-- institution_memberships table via the provider_id column added in V134, which is what
-- OwnershipGuard.verifyProvider actually enforces.
--
-- Safety: the drop is guarded and only proceeds when the table is empty, so no data can be lost.
-- If a future environment somehow holds rows, the table is preserved and a NOTICE is raised.

DO $$
DECLARE
    leftover BIGINT;
BEGIN
    IF to_regclass('public.provider_memberships') IS NULL THEN
        RAISE NOTICE 'V147: provider_memberships does not exist; nothing to do';
        RETURN;
    END IF;

    SELECT count(*) INTO leftover FROM provider_memberships;

    IF leftover > 0 THEN
        RAISE NOTICE 'V147: provider_memberships still has % row(s); leaving the table in place', leftover;
        RETURN;
    END IF;

    DROP TABLE IF EXISTS provider_memberships;
    RAISE NOTICE 'V147: dropped the orphaned, empty provider_memberships table';
END $$;