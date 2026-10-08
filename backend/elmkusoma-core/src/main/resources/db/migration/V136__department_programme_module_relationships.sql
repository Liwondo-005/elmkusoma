-- V136: real Department -> Programme -> Module relationships, plus module codes.
--
-- The academic hierarchy was not persisted:
--   * departments.programme_ids is a jsonb array, so a programme could not be queried,
--     filtered or constrained by department, and nothing enforced the link;
--   * course_modules had no module_code at all, although student progress
--     (learning_modules.module_code) already carries one.
--
-- This adds the normalised foreign keys and backfills the department link from the
-- existing jsonb array so no data is lost. The jsonb column is intentionally left in
-- place (additive-only migration discipline): it is no longer the source of truth.
--
-- One programme belongs to at most one department (matching the worked example,
-- "Department of Computer Studies -> Computer Engineering"). Modules inherit the
-- department through their course/programme, and gain their own module_code.

ALTER TABLE programmes
    ADD COLUMN IF NOT EXISTS department_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_programmes_department'
    ) THEN
        ALTER TABLE programmes
            ADD CONSTRAINT fk_programmes_department
            FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL;
    END IF;
END$$;

-- Backfill: adopt the links that departments already declared in programme_ids.
UPDATE programmes p
SET department_id = d.department_id
FROM (
    SELECT
        (jsonb_array_elements_text(d.programme_ids))::uuid AS programme_id,
        d.id AS department_id
    FROM departments d
    WHERE d.programme_ids IS NOT NULL
      AND jsonb_typeof(d.programme_ids) = 'array'
) d
WHERE p.id = d.programme_id
  AND p.department_id IS NULL;

-- Programme lookups are always department-scoped, and one department has many programmes.
CREATE INDEX IF NOT EXISTS idx_programmes_department_id
    ON programmes (department_id);

-- Module code: required by the academic model for programme-style modules. Nullable so
-- existing modules and standalone provider courses (which must NOT be forced into
-- university-style codes) keep working untouched.
ALTER TABLE course_modules
    ADD COLUMN IF NOT EXISTS module_code VARCHAR(50) NULL;

CREATE INDEX IF NOT EXISTS idx_course_modules_course_code
    ON course_modules (course_id, module_code);