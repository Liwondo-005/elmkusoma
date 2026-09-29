-- V110: Teaching & Learning Ecosystem — additive connection columns
-- All statements are additive. No existing column/table is altered destructively.

-- ── Assignments: lesson link + lifecycle rules ────────────────────────────────
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS lesson_id UUID;
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS open_date TIMESTAMP;
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS close_date TIMESTAMP;
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS allow_late_submission BOOLEAN DEFAULT FALSE;

-- ── Assignment submissions: persisted body, workflow states, draft/resubmit ───
-- (V29 declared these but was a no-op after V11 created the table first.)
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS submission_text TEXT;
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'SUBMITTED';
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS is_draft BOOLEAN DEFAULT FALSE;
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS is_late BOOLEAN DEFAULT FALSE;

-- ── Assessments: lesson link + attempt rule ──────────────────────────────────
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS lesson_id UUID;
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS max_attempts INTEGER;

-- ── Live classes: optional lesson link (Lesson ↔ Live Class connection) ──────
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS lesson_id UUID;

-- ── Report cards: class membership for per-class rank computation ────────────
ALTER TABLE report_cards ADD COLUMN IF NOT EXISTS class_group_id UUID;

-- ── Rubric tables: align with the entity/API contract (they were never written to) ──
-- grading_rubrics.title is unmapped by the entity (name carries the value) and
-- subject_id is optional in the API — relax so rubric creation can persist.
ALTER TABLE grading_rubrics ALTER COLUMN title SET DEFAULT '';
ALTER TABLE grading_rubrics ALTER COLUMN subject_id DROP NOT NULL;

-- ── Indexes for the new lookup paths ─────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_assignments_lesson_id ON assignments (lesson_id);
CREATE INDEX IF NOT EXISTS idx_assignments_class_status ON assignments (class_group_id, status);
CREATE INDEX IF NOT EXISTS idx_assignment_submissions_assignment_status
    ON assignment_submissions (assignment_id, status);
CREATE INDEX IF NOT EXISTS idx_assignment_submissions_student
    ON assignment_submissions (student_id);
CREATE INDEX IF NOT EXISTS idx_assessments_lesson_id ON assessments (lesson_id);
CREATE INDEX IF NOT EXISTS idx_live_classes_lesson_id ON live_classes (lesson_id);
CREATE INDEX IF NOT EXISTS idx_report_cards_class_group ON report_cards (class_group_id);
CREATE INDEX IF NOT EXISTS idx_report_cards_student_term ON report_cards (student_id, term_id);
