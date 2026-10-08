-- ============================================================================
-- SEED FILE: E2E Test Account Alignment
-- Aligns database with E2E test expectations
-- Password for all accounts: password (the shared seed convention)
-- Except teacher1/student1 which use "Test123!" per E2E test expectations
-- ============================================================================

-- ============================================================
-- Missing E2E test users
-- ============================================================

-- p0-workspace.spec.ts expects: john@student.test / password (STUDENT)
INSERT INTO users (id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, learning_level, created_at, updated_at, is_deleted) VALUES
('cccc1111-1111-1111-1111-111111111101', 'john@student.test', '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2', 'John', NULL, 'Student', '+255700000010', 'STUDENT', true, true, NULL, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- learning-content.spec.ts expects: audit-test@test.com / password (STUDENT)
INSERT INTO users (id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, learning_level, created_at, updated_at, is_deleted) VALUES
('cccc1111-1111-1111-1111-111111111102', 'audit-test@test.com', '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2', 'Audit', NULL, 'Test', '+255700000011', 'STUDENT', true, true, NULL, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- oversight.spec.ts expects: national@elmkusoma.go.tz / password (NATIONAL_ADMIN)
-- Note: seed_oversight.sql has national@test.com, but E2E expects national@elmkusoma.go.tz
INSERT INTO users (id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, region_id, learning_level, created_at, updated_at, is_deleted) VALUES
('dddd1111-1111-1111-1111-111111111101', 'national@elmkusoma.go.tz', '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2', 'National', NULL, 'Admin', '+255700000001', 'NATIONAL_ADMIN', true, true, '11111111-1111-1111-1111-111111111101', NULL, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- oversight.spec.ts expects regional@elmkusoma.go.tz / password (REGIONAL_ADMIN)
-- Note: seed_oversight.sql has regional.dar@test.com, but E2E expects regional@elmkusoma.go.tz
INSERT INTO users (id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, region_id, learning_level, created_at, updated_at, is_deleted) VALUES
('dddd1111-1111-1111-1111-111111111102', 'regional@elmkusoma.go.tz', '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2', 'Regional', NULL, 'Admin', '+255700000002', 'REGIONAL_ADMIN', true, true, '11111111-1111-1111-1111-111111111102', NULL, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- oversight.spec.ts expects: student1@darms.edu.tz / password (STUDENT/LEARNER)
-- Note: TestDataSeeder has student@elmkusoma.tz, but E2E uses student1@darms.edu.tz
-- Also E2E uses "password" for UI login but Test123! for API (see offerings-smoke.spec.ts)
INSERT INTO users (id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, learning_level, created_at, updated_at, is_deleted) VALUES
('eeee1111-1111-1111-1111-111111111101', 'student1@darms.edu.tz', '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2', 'Student', NULL, 'One', '+255700000003', 'STUDENT', true, true, NULL, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- p0-workspace.spec.ts expects: admin@darms.edu.tz / password (INSTITUTION_ADMIN)
INSERT INTO users (id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, learning_level, created_at, updated_at, is_deleted) VALUES
('ffff1111-1111-1111-1111-111111111101', 'admin@darms.edu.tz', '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2', 'Admin', NULL, 'Dar', '+255700000004', 'INSTITUTION_ADMIN', true, true, NULL, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- offerings-smoke.spec.ts expects: teacher1@darms.edu.tz / Test123! (TEACHER)
-- Note: This user uses "Test123!" password, different from standard "password"
INSERT INTO users (id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, learning_level, created_at, updated_at, is_deleted) VALUES
('ffff1111-1111-1111-1111-111111111102', 'teacher1@darms.edu.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Teacher', NULL, 'One', '+255700000005', 'TEACHER', true, true, NULL, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- p0-workspace.spec.ts expects: student1@darms.edu.tz (STUDENT)
-- Also used in regional-admin.spec.ts with password "password"
-- Note: Already added above as student1@darms.edu.tz

-- ============================================================
-- Institution memberships for new users
-- ============================================================

-- john@student.test -> institution A
INSERT INTO institution_memberships (id, user_id, institution_id, role, is_active, created_at, updated_at, is_deleted)
VALUES ('mmmm1111-1111-1111-1111-111111111101', 'cccc1111-1111-1111-1111-111111111101', 'a0000000-0000-0000-0000-000000000002', 'STUDENT', true, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- audit-test@test.com -> institution A
INSERT INTO institution_memberships (id, user_id, institution_id, role, is_active, created_at, updated_at, is_deleted)
VALUES ('mmmm1111-1111-1111-1111-111111111102', 'cccc1111-1111-1111-1111-111111111102', 'a0000000-0000-0000-0000-000000000001', 'STUDENT', true, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- national@elmkusoma.go.tz -> institution A (NATIONAL_ADMIN)
INSERT INTO institution_memberships (id, user_id, institution_id, role, is_active, created_at, updated_at, is_deleted)
VALUES ('mmmm1111-1111-1111-1111-111111111103', 'dddd1111-1111-1111-1111-111111111101', 'a0000000-0000-0000-0000-000000000001', 'NATIONAL_ADMIN', true, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- regional@elmkusoma.go.tz -> institution A (REGIONAL_ADMIN)
INSERT INTO institution_memberships (id, user_id, institution_id, role, is_active, created_at, updated_at, is_deleted)
VALUES ('mmmm1111-1111-1111-1111-111111111104', 'dddd1111-1111-1111-1111-111111111102', 'a0000000-0000-0000-0000-000000000001', 'REGIONAL_ADMIN', true, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- student1@darms.edu.tz -> institution B (STUDENT)
INSERT INTO institution_memberships (id, user_id, institution_id, role, is_active, created_at, updated_at, is_deleted)
VALUES ('mmmm1111-1111-1111-1111-111111111105', 'eeee1111-1111-1111-1111-111111111101', 'a0000000-0000-0000-0000-000000000002', 'STUDENT', true, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- admin@darms.edu.tz -> institution B (INSTITUTION_ADMIN)
INSERT INTO institution_memberships (id, user_id, institution_id, role, is_active, created_at, updated_at, is_deleted)
VALUES ('mmmm1111-1111-1111-1111-111111111106', 'ffff1111-1111-1111-1111-111111111101', 'a0000000-0000-0000-0000-000000000002', 'ADMIN', true, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- teacher1@darms.edu.tz -> institution B (TEACHER)
INSERT INTO institution_memberships (id, user_id, institution_id, role, is_active, created_at, updated_at, is_deleted)
VALUES ('mmmm1111-1111-1111-1111-111111111107', 'ffff1111-1111-1111-1111-111111111102', 'a0000000-0000-0000-0000-000000000002', 'TEACHER', true, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Student profiles for new student users
-- ============================================================

-- john@student.test student profile
INSERT INTO students (id, user_id, institution_id, admission_number, first_name, last_name, phone, gender, date_of_birth, address, city, region, national_id, blood_group, medical_notes, guardian_name, guardian_phone, guardian_relationship, guardian_address, guardian_city, guardian_region, guardian_national_id, is_active, is_deleted, created_at, updated_at)
VALUES ('nnnn1111-1111-1111-1111-111111111101', 'cccc1111-1111-1111-1111-111111111101', 'a0000000-0000-0000-0000-000000000001', 'STU-JOHN-001', 'John', 'Student', '+255700000010', 'MALE', '2010-01-15', '123 Student St', 'Dar es Salaam', 'Dar es Salaam', '123456789012', 'O+', NULL, 'Jane Student', '+255700000011', 'Mother', '123 Student St', 'Dar es Salaam', 'Dar es Salaam', '123456789013', true, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- audit-test@test.com student profile
INSERT INTO students (id, user_id, institution_id, admission_number, first_name, last_name, phone, gender, date_of_birth, address, city, region, national_id, blood_group, medical_notes, guardian_name, guardian_phone, guardian_relationship, guardian_address, guardian_city, guardian_region, guardian_national_id, is_active, is_deleted, created_at, updated_at)
VALUES ('nnnn1111-1111-1111-1111-111111111102', 'cccc1111-1111-1111-1111-111111111102', 'a0000000-0000-0000-0000-000000000001', 'STU-AUDIT-001', 'Audit', 'Test', '+255700000011', 'FEMALE', '2010-02-20', '456 Audit Ave', 'Dar es Salaam', 'Dar es Salaam', '123456789014', 'A+', NULL, 'Guardian Test', '+255700000012', 'Father', '456 Audit Ave', 'Dar es Salaam', 'Dar es Salaam', '123456789015', true, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- student1@darms.edu.tz student profile
INSERT INTO students (id, user_id, institution_id, admission_number, first_name, last_name, phone, gender, date_of_birth, address, city, region, national_id, blood_group, medical_notes, guardian_name, guardian_phone, guardian_relationship, guardian_address, guardian_city, guardian_region, guardian_national_id, is_active, is_deleted, created_at, updated_at)
VALUES ('nnnn1111-1111-1111-1111-111111111103', 'eeee1111-1111-1111-1111-111111111101', 'a0000000-0000-0000-0000-000000000002', 'STU-S1-001', 'Student', 'One', '+255700000003', 'MALE', '2010-03-10', '789 Student Rd', 'Dar es Salaam', 'Dar es Salaam', '123456789016', 'B+', NULL, 'Guardian One', '+255700000013', 'Mother', '789 Student Rd', 'Dar es Salaam', 'Dar es Salaam', '123456789017', true, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Teacher profile for teacher1@darms.edu.tz
-- ============================================================
INSERT INTO teachers (id, user_id, institution_id, employee_number, specialization, status, hire_date, bio, is_deleted, created_at, updated_at)
VALUES ('tttt1111-1111-1111-1111-111111111101', 'ffff1111-1111-1111-1111-111111111102', 'a0000000-0000-0000-0000-000000000002', 'EMP-T1-001', 'Mathematics', 'ACTIVE', '2020-01-15', 'Experienced mathematics teacher', FALSE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Parent profile for parent of student1@darms.edu.tz (for regional-admin tests)
-- ============================================================
INSERT INTO parents (id, user_id, institution_id, relationship_type, is_active, is_deleted, created_at, updated_at)
VALUES ('pppp1111-1111-1111-1111-111111111101', 'eeee1111-1111-1111-1111-111111111101', 'a0000000-0000-0000-0000-000000000002', 'GUARDIAN', TRUE, FALSE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Link parent to student1
INSERT INTO parent_student_links (id, parent_id, student_id, relationship_type, is_primary, is_deleted, created_at, updated_at)
VALUES ('llll1111-1111-1111-1111-111111111101', 'pppp1111-1111-1111-1111-111111111101', 'nnnn1111-1111-1111-1111-111111111103', 'GUARDIAN', TRUE, FALSE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;