-- V174: parent_student_links integrity - student_id must reference students.id.
--
-- The defect this closes is real, not theoretical. Every reader of this table resolves the value
-- as a student record: ParentDashboardServiceImpl calls getStudent(link.getStudentId()), and
-- ParentAuthorizationService, StudentController, CertificateController and EnrollmentServiceImpl
-- all treat the same UUID as a students.id when deciding whether a parent may see or buy for a
-- child. parent_student_links had a foreign key to parents(id) and to institutions(id) but none
-- to students(id), so nothing stopped a row storing something else entirely - and the seeded
-- rows did exactly that, holding the student's *user* id while no students row with that id
-- existed. The visible symptom is that the parent's child list cannot be resolved at all.
--
-- Three forward-only steps. No column is dropped or retyped, no history is rewritten, and the
-- constraint is added NOT VALID so an environment that still holds legacy rows is not blocked
-- from starting while its data is reconciled by hand.
--
-- Additive and safe to re-run: every statement is guarded by IF NOT EXISTS / a NOT EXISTS test.

-- 1. Repair rows that can be repaired. A student record is tied to its login through
--    students.user_id, so a link storing that user id is unambiguously pointing one column to the
--    wrong side of the relationship and can be remapped. The NOT EXISTS guard makes this a no-op
--    for any row that already holds a valid students.id, so it is idempotent, and it cannot touch
--    a row whose id is already valid.
UPDATE parent_student_links l
   SET student_id = s.id
  FROM students s
 WHERE s.user_id = l.student_id
   AND s.id <> l.student_id
   AND l.is_deleted = false
   AND NOT EXISTS (SELECT 1 FROM students s2 WHERE s2.id = l.student_id);

-- 2. Support the lookup every reader performs (findAllByStudentId), which had no index and fell
--    back to a sequential scan of the table.
CREATE INDEX IF NOT EXISTS idx_parent_student_links_student
    ON parent_student_links (student_id)
    WHERE is_deleted = false;

-- 3. Make the relationship enforceable from now on.
--
--    NOT VALID is the deliberate choice: Postgres still enforces the constraint on every INSERT
--    and UPDATE, so the defect cannot be reintroduced, but it does not scan and reject legacy
--    rows that exist today. A deployment can reconcile those in its own time and then run
--    VALIDATE CONSTRAINT, which is an operator decision about real relationship data rather than
--    something a schema migration should decide for it.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'parent_student_links'::regclass
          AND conname = 'fk_parent_student_links_student'
    ) THEN
        ALTER TABLE parent_student_links
            ADD CONSTRAINT fk_parent_student_links_student
            FOREIGN KEY (student_id) REFERENCES students(id) NOT VALID;
    END IF;
END
$$;
