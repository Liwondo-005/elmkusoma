-- ============================================================================
-- R0 SEED ALIGNMENT v2 — ELMKUSOMA E2E test environment
--
-- Corrected, idempotent replacement for the earlier partial attempt
-- (seed_e2e_alignment.sql / fix_passwords*.sql in this directory aborted
-- mid-file on schema mismatches: students has no first_name/last_name/phone,
-- parents has no is_active, one statement used the V122 BROKEN-PLACEHOLDER
-- hash instead of BCrypt('password')).
--
-- Ground truth:
--   * BCrypt('password') = $2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2
--     (confirmed by V122__reset_broken_seed_password_hashes.sql)
--   * E2E specs (tracked) expect: john@student.test, audit-test@test.com,
--     regional.dar@test.com, district.ila@test.com, national/regional@elmkusoma.go.tz,
--     admin@darms.edu.tz, student1@darms.edu.tz, teacher1@darms.edu.tz — all / password
--   * Learner resources + video tutorials are institution-scoped server-side
--     (LearnerController.browseResources / VideoTutorialService.list),
--     so learners need users.institution_id + membership + matching fixtures.
--
-- Only adds/aligns LOCAL DEV test data. No application code, no auth logic,
-- no production data touched.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1) teacher1@darms.edu.tz currently carries a manual 'Test123!' hash; the
--    tracked E2E specs (live-player.spec.ts, offerings-smoke.spec.ts) log in
--    with the seed convention password. Align the credential only.
-- ---------------------------------------------------------------------------
UPDATE users
SET password_hash = '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2',
    updated_at    = NOW()
WHERE email = 'teacher1@darms.edu.tz'
  AND password_hash <> '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2';

-- ---------------------------------------------------------------------------
-- 2) john / audit-test exist but have no institution (created by the earlier
--    partial attempt). Put them in the Dar Model School household where
--    teacher1/student1 already live.
-- ---------------------------------------------------------------------------
UPDATE users
SET institution_id = 'a0000000-0000-0000-0000-000000000002',
    updated_at     = NOW()
WHERE email IN ('john@student.test', 'audit-test@test.com')
  AND institution_id IS NULL;

-- ---------------------------------------------------------------------------
-- 2b) The learner dashboard layout (app/dashboard/learner/layout.tsx:86) gates
--     /dashboard/learner/** to role "Other Learner" OR Student with
--     learning_level in (COLLEGE, UNIVERSITY, VETA); audit-test had NULL and
--     rendered "Access denied" on the resources/video-library pages the tracked
--     learning-content.spec.ts asserts. Learning level is profile data (users.learning_level).
UPDATE users
SET learning_level = 'COLLEGE',
    updated_at     = NOW()
WHERE email = 'audit-test@test.com'
  AND (learning_level IS NULL OR learning_level <> 'COLLEGE');

-- 3) Institution memberships (STUDENT, active) — mirrors student1's shape.
-- ---------------------------------------------------------------------------
INSERT INTO institution_memberships (user_id, institution_id, role, is_active, created_at, updated_at, is_deleted)
SELECT u.id, 'a0000000-0000-0000-0000-000000000002', 'STUDENT', true, NOW(), NOW(), false
FROM users u
WHERE u.email IN ('john@student.test', 'audit-test@test.com')
  AND NOT EXISTS (
    SELECT 1 FROM institution_memberships m
    WHERE m.user_id = u.id
      AND m.institution_id = 'a0000000-0000-0000-0000-000000000002'
      AND m.is_deleted = false
  );

-- ---------------------------------------------------------------------------
-- 4) Student profiles (schema: names/phone live on users, NOT students;
--    status/enrollment_date have defaults; admission_number derives from user id)
-- ---------------------------------------------------------------------------
INSERT INTO students (id, institution_id, user_id, admission_number, is_deleted, created_at, updated_at)
SELECT gen_random_uuid(),
       'a0000000-0000-0000-0000-000000000002',
       u.id,
       'STU-E2E-' || right(replace(u.id::text, '-', ''), 8),
       false, NOW(), NOW()
FROM users u
WHERE u.email IN ('john@student.test', 'audit-test@test.com')
  AND NOT EXISTS (SELECT 1 FROM students s WHERE s.user_id = u.id AND s.is_deleted = false);

-- ---------------------------------------------------------------------------
-- 5) The three jurisdiction test institutions from the TRACKED
--    backend/seed_oversight.sql (users part was applied, institutions part
--    never was — regional-admin.spec.ts asserts them by name).
-- ---------------------------------------------------------------------------
INSERT INTO institutions (id, name, code, type, description, address, city, region, country, phone, email, is_active, created_at, updated_at, is_deleted, region_id, district_id)
VALUES
  ('bbbb1111-1111-1111-1111-111111111101', 'Test Primary School Ilala', 'TPS-ILA', 'PRIMARY', 'Test primary school in Ilala district', '123 Main St', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', '+255700000010', 'tpsilala@test.com', true, NOW(), NOW(), false, '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222201'),
  ('bbbb1111-1111-1111-1111-111111111102', 'Test Secondary School Kinondoni', 'TSS-KIN', 'SECONDARY', 'Test secondary school in Kinondoni district', '456 Academy Rd', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', '+255700000011', 'tsskinondoni@test.com', true, NOW(), NOW(), false, '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222202'),
  ('bbbb1111-1111-1111-1111-111111111103', 'Test Primary School Arusha', 'TPS-ARC', 'PRIMARY', 'Test primary school in Arusha City', '789 Education Lane', 'Arusha', 'Arusha', 'Tanzania', '+255700000012', 'tpsarusha@test.com', true, NOW(), NOW(), false, '11111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222205')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6) learning-content.spec.ts content fixtures (resources + video_tutorials
--    tables were EMPTY — no seed anywhere creates these titles). Scoped to
--    the same institution as audit-test, student-readable visibility.
-- ---------------------------------------------------------------------------
-- NOTE: chk_resource_file requires storage_url for non-link resource types.
INSERT INTO resources (id, institution_id, title, description, resource_type, visibility,
                       is_downloadable, is_previewable, created_by, uploaded_by, storage_url,
                       processing_status, created_at, updated_at, is_deleted)
VALUES ('11111111-1111-1111-1111-11111111e2e1',
        'a0000000-0000-0000-0000-000000000002',
        'Test Resource',
        'E2E learning-content fixture resource',
        'DOCUMENT', 'INSTITUTION', true, true, 'e2e-seed',
        'cccc1111-1111-1111-1111-111111111102',
        'https://e2e-fixtures.local/test-resource.pdf',
        'READY', NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- uploaded_by must be a non-null users.id: ResourceService.mapToResponse calls
-- userRepository.findById(getUploadedBy()) unguarded (line ~1401) and a null id
-- 500s the whole learner resources listing ("The given id must not be null").
UPDATE resources
SET uploaded_by = 'cccc1111-1111-1111-1111-111111111102'
WHERE id = '11111111-1111-1111-1111-11111111e2e1'
  AND uploaded_by IS NULL;

INSERT INTO video_tutorials (id, institution_id, created_by, title, description, status, visibility,
                             duration_seconds, is_downloadable, is_previewable, created_at, updated_at, is_deleted)
VALUES ('11111111-1111-1111-1111-11111111e2e2',
        'a0000000-0000-0000-0000-000000000002',
        'e2e-seed',
        'Test Video Tutorial',
        'E2E learning-content fixture video',
        'READY', 'INSTITUTION', 120, true, true, NOW(), NOW(), false)
ON CONFLICT (id) DO NOTHING;
