-- V124: Class-targeted resources point at their authoritative TeacherAssignment.
-- Additive and nullable: legacy resources keep their existing targeting
-- (lesson/module/course + visibility) untouched; only new writes are required
-- to provide an authoritative target for CLASS_ONLY visibility.
-- NO ACTION (restrict) on purpose: assignments are ended, never hard-deleted,
-- so a resource can never lose its target silently.

ALTER TABLE resources
    ADD COLUMN IF NOT EXISTS teacher_assignment_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conname = 'fk_resources_teacher_assignment'
    ) THEN
        ALTER TABLE resources
            ADD CONSTRAINT fk_resources_teacher_assignment
            FOREIGN KEY (teacher_assignment_id)
            REFERENCES teacher_assignments (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_resources_teacher_assignment
    ON resources (teacher_assignment_id)
    WHERE is_deleted = false;
