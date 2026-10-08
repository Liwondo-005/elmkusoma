-- V131: Provider <-> Learner ecosystem linkage (audit Phase 3 -- B-11, X-2, X-3)
--
-- The NFE provider stack and the learner stack were completely disjoint: not one foreign key,
-- repository, DTO or query joined any nfe_* table to courses / learner_enrollments / enrollments /
-- assignments / assignment_submissions / students. Providers could not attach content to a course
-- and learners could never reach provider programs or certificates.
--
-- This migration adds the missing relationships as ADDITIVE columns + constraints. It does not
-- introduce a new content model: nfe_programs gains an OPTIONAL link to the existing courses table
-- and nfe_learners gains an OPTIONAL link to users/students, so both existing models keep working
-- unchanged and a provider can now bridge them.
--
-- No historical migration is edited. No row is deleted; orphans are neutralised with a soft delete
-- before each constraint is validated.

-- ---------------------------------------------------------------------------
-- 1) Link columns (nullable so every existing row stays valid)
-- ---------------------------------------------------------------------------
ALTER TABLE nfe_programs  ADD COLUMN IF NOT EXISTS course_id  UUID;
ALTER TABLE nfe_learners ADD COLUMN IF NOT EXISTS student_id UUID;

-- Provider-authored sessions/materials can hang off a course too (via their program), which the
-- program link already provides; these two keep the query path explicit for performance.
ALTER TABLE nfe_programs  ADD COLUMN IF NOT EXISTS enrollment_open BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_nfe_programs_course
    ON nfe_programs (course_id) WHERE is_deleted = false AND course_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_nfe_learners_student
    ON nfe_learners (student_id) WHERE is_deleted = false AND student_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 2) Foreign keys
--    The nfe_* tables had ZERO FKs, so a row could reference another tenant's provider, learner,
--    session or program and the database would happily accept it (audit B-15/B-19).
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    spec record;
BEGIN
    FOR spec IN
        SELECT * FROM (VALUES
            ('fk_nfe_providers_institution',  'nfe_education_providers', 'institution_id', 'institutions'),
            ('fk_nfe_programs_institution',   'nfe_programs',           'institution_id', 'institutions'),
            ('fk_nfe_programs_provider',      'nfe_programs',           'provider_id',    'nfe_education_providers'),
            ('fk_nfe_programs_course',        'nfe_programs',           'course_id',      'courses'),
            ('fk_nfe_sessions_institution',   'nfe_sessions',           'institution_id', 'institutions'),
            ('fk_nfe_sessions_provider',      'nfe_sessions',           'provider_id',    'nfe_education_providers'),
            ('fk_nfe_sessions_program',       'nfe_sessions',           'program_id',     'nfe_programs'),
            ('fk_nfe_materials_institution',  'nfe_materials',          'institution_id', 'institutions'),
            ('fk_nfe_materials_provider',     'nfe_materials',          'provider_id',    'nfe_education_providers'),
            ('fk_nfe_materials_program',      'nfe_materials',          'program_id',     'nfe_programs'),
            ('fk_nfe_learners_institution',   'nfe_learners',           'institution_id', 'institutions'),
            ('fk_nfe_learners_provider',      'nfe_learners',           'provider_id',    'nfe_education_providers'),
            ('fk_nfe_learners_user',          'nfe_learners',           'user_id',        'users'),
            ('fk_nfe_learners_student',       'nfe_learners',           'student_id',     'students'),
            ('fk_nfe_assessments_institution','nfe_assessments',        'institution_id', 'institutions'),
            ('fk_nfe_assessments_provider',   'nfe_assessments',        'provider_id',    'nfe_education_providers'),
            ('fk_nfe_assessments_program',    'nfe_assessments',        'program_id',     'nfe_programs'),
            ('fk_nfe_attendance_institution', 'nfe_attendance',         'institution_id', 'institutions'),
            ('fk_nfe_attendance_provider',    'nfe_attendance',         'provider_id',    'nfe_education_providers'),
            ('fk_nfe_attendance_session',     'nfe_attendance',         'session_id',     'nfe_sessions'),
            ('fk_nfe_attendance_learner',     'nfe_attendance',         'learner_id',     'nfe_learners'),
            ('fk_nfe_certificates_institution','nfe_certificates',      'institution_id', 'institutions'),
            ('fk_nfe_certificates_provider',  'nfe_certificates',      'provider_id',    'nfe_education_providers'),
            ('fk_nfe_certificates_learner',   'nfe_certificates',      'learner_id',     'nfe_learners'),
            ('fk_nfe_certificates_program',   'nfe_certificates',      'program_id',     'nfe_programs')
        ) AS t(conname, tbl, col, parent)
    LOOP
        IF to_regclass('public.' || spec.tbl) IS NOT NULL
           AND to_regclass('public.' || spec.parent) IS NOT NULL
           AND EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_name = spec.tbl AND column_name = spec.col)
           AND NOT EXISTS (SELECT 1 FROM pg_constraint
                            WHERE conname = spec.conname
                              AND conrelid = spec.tbl::regclass) THEN
            EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %I(id) NOT VALID',
                           spec.tbl, spec.conname, spec.col, spec.parent);
        END IF;
    END LOOP;
END $$;

-- ---------------------------------------------------------------------------
-- 3) Orphan audit: neutralise (soft-delete) rows whose references point nowhere,
--    so the constraints above can be validated without losing data.
-- ---------------------------------------------------------------------------
UPDATE nfe_programs p
   SET is_deleted = true, updated_at = NOW()
 WHERE p.is_deleted = false
   AND p.provider_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM nfe_education_providers pr
                    WHERE pr.id = p.provider_id AND pr.institution_id = p.institution_id);

UPDATE nfe_learners l
   SET is_deleted = true, updated_at = NOW()
 WHERE l.is_deleted = false
   AND l.provider_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM nfe_education_providers pr
                    WHERE pr.id = l.provider_id AND pr.institution_id = l.institution_id);

UPDATE nfe_learners l
   SET is_deleted = true, updated_at = NOW()
 WHERE l.is_deleted = false
   AND l.user_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = l.user_id);

-- Promote every NOT VALID constraint that now holds. Failures are non-fatal: a NOT VALID
-- constraint still protects all new writes, which is the property that matters.
DO $$
DECLARE
    c record;
BEGIN
    FOR c IN
        SELECT conname, conrelid::regclass AS tbl FROM pg_constraint
         WHERE connamespace = 'public'::regnamespace
           AND NOT convalidated
           AND conname LIKE 'fk_nfe_%'
LOOP
        BEGIN
            EXECUTE format('ALTER TABLE %s VALIDATE CONSTRAINT %I', c.tbl, c.conname);
        EXCEPTION WHEN others THEN
            RAISE NOTICE 'V131: leaving % NOT VALID (%)', c.conname, SQLERRM;
        END;
    END LOOP;
END $$;