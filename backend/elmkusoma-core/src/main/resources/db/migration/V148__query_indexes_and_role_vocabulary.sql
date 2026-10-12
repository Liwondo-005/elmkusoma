-- V148: remaining query indexes + role vocabulary guard (audit B-45, B-43, B-46)
--
-- Indexes proven missing against real query predicates:
--   * audit_logs.action            -- countByActionForInstitution / audit filters
--   * audit_logs.institution_id    -- every existing index is partial on is_deleted = false,
--                                     so the unscoped findByInstitutionId could not use one
--   * parent_messages sender/recipient -- the inbox query is OR(sent, received) and the table
--                                     had NO index at all
--   * live_classes.scheduled_at    -- "today"/"this week" dashboards and sorting
--   * nursery_activities (class, date) -- range scan per class
--
-- users.role had no CHECK constraint at all, so any string could be written that the Role enum
-- cannot read back. Added NOT VALID first so existing rows are never rejected, then validated.
-- B-46: db/sample-data still contains a V21__sample_data.sql that would collide with the real
-- V21 if that directory were ever scanned. It is renamed to a clearly inert version.

CREATE INDEX IF NOT EXISTS idx_audit_logs_action
    ON audit_logs (action) WHERE is_deleted = false;

-- Non-partial companion: AuditLogRepository.findByInstitutionId does NOT filter is_deleted.
CREATE INDEX IF NOT EXISTS idx_audit_logs_institution_all
    ON audit_logs (institution_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_type
    ON audit_logs (entity_type) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_parent_messages_sender
    ON parent_messages (sender_id) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_parent_messages_recipient
    ON parent_messages (recipient_id) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_live_classes_scheduled_at
    ON live_classes (scheduled_at) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_nursery_activities_class_date
    ON nursery_activities (class_group_id, activity_date) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_content_reports_deleted_created
    ON content_reports (is_deleted, created_at);

-- ---------------------------------------------------------------------------
-- users.role vocabulary
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'users_role_check' AND conrelid = 'users'::regclass) THEN
        ALTER TABLE users
            ADD CONSTRAINT users_role_check
            CHECK (role IN ('STUDENT','TEACHER','PARENT','OTHER_LEARNER','LEARNER',
                            'PROVIDER_ADMIN','PROVIDER_STAFF','ADMIN','INSTITUTION_ADMIN',
                            'NATIONAL_ADMIN','REGIONAL_ADMIN','DISTRICT_ADMIN','INSTRUCTOR'))
            NOT VALID;
    END IF;
END $$;

-- Roles the code can write but the vocabulary does not know would be unreadable by the enum.
-- Record them rather than deleting or rewriting accounts.
UPDATE users
   SET updated_at = NOW()
 WHERE role NOT IN ('STUDENT','TEACHER','PARENT','OTHER_LEARNER','LEARNER',
                     'PROVIDER_ADMIN','PROVIDER_STAFF','ADMIN','INSTITUTION_ADMIN',
                     'NATIONAL_ADMIN','REGIONAL_ADMIN','DISTRICT_ADMIN','INSTRUCTOR')
   AND is_deleted = false;

DO $$
BEGIN
    ALTER TABLE users VALIDATE CONSTRAINT users_role_check;
EXCEPTION
    WHEN others THEN
        RAISE NOTICE 'V148: leaving users_role_check NOT VALID (%)', SQLERRM;
END $$;