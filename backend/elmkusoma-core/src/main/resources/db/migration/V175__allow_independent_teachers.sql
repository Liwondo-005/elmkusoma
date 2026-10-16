-- Independent teachers and open learning sessions (requirement A/B).
--
-- Since V04/V08 both teachers.institution_id and live_classes.institution_id were NOT NULL,
-- every teacher — and every session they hosted — had to name a school. The platform now
-- distinguishes a teacher's institutional affiliation from their teaching audience:
-- school-affiliated teachers keep their school exactly as before, while independent
-- teachers (and the open learning sessions they host) are institution-less by design.
--
-- Backward compatible: every existing row keeps its value; only NULL becomes legal.
-- The foreign keys stay in place (PostgreSQL allows NULL in a FK column), so an
-- institution-scoped teacher/class can never point at a missing school.

ALTER TABLE teachers ALTER COLUMN institution_id DROP NOT NULL;
ALTER TABLE live_classes ALTER COLUMN institution_id DROP NOT NULL;

-- V04 pinned (user_id, institution_id) unique; in PostgreSQL NULLs are distinct in a
-- unique constraint, so that alone would let one user accumulate several institution-less
-- profile rows. Pin at most one independent profile per user — the same intent V04 had
-- for affiliated teachers, extended to the independent case.
CREATE UNIQUE INDEX IF NOT EXISTS uq_teachers_user_independent
    ON teachers (user_id)
    WHERE institution_id IS NULL;
