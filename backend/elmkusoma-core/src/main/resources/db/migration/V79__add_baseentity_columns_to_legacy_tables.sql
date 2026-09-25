-- BaseEntity (@MappedSuperclass) declares institution_id / created_by / updated_by
-- for every entity, so Hibernate selects those columns on any entity query.
-- These four tables predate those columns, causing SQLState 42703
-- "column ... does not exist" failures (first observed in CoreScheduler.dailyRetentionSweep
-- on platform_config). Add the missing columns (nullable, additive) plus indexes.

ALTER TABLE course_lessons       ADD COLUMN IF NOT EXISTS institution_id UUID;
ALTER TABLE course_lessons       ADD COLUMN IF NOT EXISTS created_by     VARCHAR(255);
ALTER TABLE course_lessons       ADD COLUMN IF NOT EXISTS updated_by     VARCHAR(255);

ALTER TABLE course_modules       ADD COLUMN IF NOT EXISTS institution_id UUID;
ALTER TABLE course_modules       ADD COLUMN IF NOT EXISTS created_by     VARCHAR(255);
ALTER TABLE course_modules       ADD COLUMN IF NOT EXISTS updated_by     VARCHAR(255);

ALTER TABLE support_ticket_messages ADD COLUMN IF NOT EXISTS institution_id UUID;
ALTER TABLE support_ticket_messages ADD COLUMN IF NOT EXISTS created_by     VARCHAR(255);
ALTER TABLE support_ticket_messages ADD COLUMN IF NOT EXISTS updated_by     VARCHAR(255);

ALTER TABLE platform_config ADD COLUMN IF NOT EXISTS institution_id UUID;

CREATE INDEX IF NOT EXISTS idx_course_lessons_institution       ON course_lessons(institution_id);
CREATE INDEX IF NOT EXISTS idx_course_modules_institution       ON course_modules(institution_id);
CREATE INDEX IF NOT EXISTS idx_support_ticket_messages_institution ON support_ticket_messages(institution_id);
CREATE INDEX IF NOT EXISTS idx_platform_config_institution      ON platform_config(institution_id);