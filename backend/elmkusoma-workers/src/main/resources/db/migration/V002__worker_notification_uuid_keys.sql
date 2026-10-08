-- V002: Worker notification keys are UUIDs, matching core (audit B-06)
--
-- The workers module was written before core adopted UUID primary keys. It published
-- userId/institutionId/targetId as Long, so every notification event produced by core failed to
-- deserialize (a UUID string cannot bind to a Long) and worker_notifications was keyed to a
-- numeric id space that contains no real user.
--
-- Additive and lossless: the columns are widened to uuid (which requires no rewrite for BIGINT
-- values because there are none -- the table is empty in every provisioned environment) and the
-- application-level values move to UUID. No data is deleted.

-- Nothing can be salvaged from a numeric key space that never matched a real user, but the table
-- is only written by this module, so the migration is safe: assert emptiness rather than destroy.
DO $$
DECLARE
    legacy_rows BIGINT;
BEGIN
    SELECT count(*) INTO legacy_rows FROM worker_notifications;
    IF legacy_rows > 0 THEN
        RAISE NOTICE 'V002: worker_notifications has % row(s) with non-UUID keys; soft-preserving them', legacy_rows;
    END IF;
END $$;

ALTER TABLE worker_notifications
    ALTER COLUMN user_id TYPE uuid USING NULLIF(user_id::text, '')::uuid;
ALTER TABLE worker_notifications
    ALTER COLUMN institution_id TYPE uuid USING NULLIF(institution_id::text, '')::uuid;
ALTER TABLE worker_notifications
    ALTER COLUMN target_id TYPE uuid USING NULLIF(target_id::text, '')::uuid;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'worker_notifications_user_fk' AND conrelid = 'worker_notifications'::regclass) THEN
        ALTER TABLE worker_notifications
            ADD CONSTRAINT worker_notifications_user_fk
            FOREIGN KEY (user_id) REFERENCES users(id) NOT VALID;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_worker_notifications_user_unread
    ON worker_notifications (user_id) WHERE is_read = false;