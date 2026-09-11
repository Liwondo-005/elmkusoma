-- ============================================================
-- SAMPLE DATA FOR ELMKUSOMA
-- All UUIDs use valid hex characters (0-9, a-f only)
-- Applied directly via psql, not as Flyway migration
-- ============================================================

-- 0. CREATE INSTITUTION (required before all other inserts reference it)
INSERT INTO institutions (id, name, code, type, is_active, created_at, is_deleted)
VALUES ('fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'Bunge Primary School', 'BPS', 'PRIMARY', true, '2026-01-10 08:00:00', false)
ON CONFLICT DO NOTHING;

-- 1. UPDATE INSTITUTION
UPDATE institutions SET
  description = 'Bunge Primary School is a leading primary education institution in Dar es Salaam, committed to academic excellence and holistic child development.',
  address = '123 Bunge Road, Mwananyamala',
  city = 'Dar es Salaam',
  region = 'Dar es Salaam',
  country = 'Tanzania',
  phone = '+255 22 123 4567',
  email = 'info@bungeschool.ac.tz',
  website = 'https://bungeschool.ac.tz',
  is_active = true
WHERE id = 'fbd2e3e3-99df-48f3-b138-58d6f6f84103';

-- 2. CREATE USERS (Admin)
INSERT INTO users (id, institution_id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, created_at, is_deleted)
VALUES (
    '22548351-f15a-4f60-b1ff-f807743ef4cc',
    'fbd2e3e3-99df-48f3-b138-58d6f6f84103',
    'admin@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'System',
    NULL,
    'Administrator',
    NULL,
    'ADMIN',
    true,
    true,
    '2026-01-10 08:00:00',
    false
);

-- 3. CREATE USERS (Teachers)
INSERT INTO users (id, institution_id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, created_at, is_deleted)
VALUES
  ('a1b2c3d4-e5f6-7890-abcd-ef1234567801', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'mary.johnson@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Mary', 'Ann', 'Johnson', '+255 712 345 001', 'TEACHER', true, true, '2026-01-15 08:00:00', false),
  ('a1b2c3d4-e5f6-7890-abcd-ef1234567802', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'james.kimaro@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'James', 'Michael', 'Kimaro', '+255 712 345 002', 'TEACHER', true, true, '2026-01-15 08:00:00', false),
  ('a1b2c3d4-e5f6-7890-abcd-ef1234567803', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'grace.mwakasege@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Grace', 'Peter', 'Mwakasege', '+255 712 345 003', 'TEACHER', true, true, '2026-02-01 08:00:00', false),
  ('a1b2c3d4-e5f6-7890-abcd-ef1234567804', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'david.ngowi@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'David', 'Samuel', 'Ngowi', '+255 712 345 004', 'TEACHER', true, true, '2026-02-01 08:00:00', false);

-- 4. CREATE USERS (Students)
INSERT INTO users (id, institution_id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, created_at, is_deleted)
VALUES
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567801', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'fatima.ali@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Fatima', 'Hassan', 'Ali', '+255 712 345 101', 'STUDENT', true, true, '2026-01-20 08:00:00', false),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567802', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'peter.mushi@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Peter', 'Joseph', 'Mushi', '+255 712 345 102', 'STUDENT', true, true, '2026-01-20 08:00:00', false),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567803', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'aisha.omar@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Aisha', 'Ibrahim', 'Omar', '+255 712 345 103', 'STUDENT', true, true, '2026-01-20 08:00:00', false),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567804', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'mark.ndunguru@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Mark', 'Stephen', 'Ndunguru', '+255 712 345 104', 'STUDENT', true, true, '2026-01-20 08:00:00', false),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567805', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'sarah.kilanga@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Sarah', 'Elizabeth', 'Kilanga', '+255 712 345 105', 'STUDENT', true, true, '2026-01-20 08:00:00', false),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567806', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'hassan.mwinyi@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Hassan', 'Ali', 'Mwinyi', '+255 712 345 106', 'STUDENT', true, true, '2026-01-20 08:00:00', false),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567807', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'rebecca.mwamba@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Rebecca', 'Joyce', 'Mwamba', '+255 712 345 107', 'STUDENT', true, true, '2026-01-20 08:00:00', false),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567808', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'daniel.kamanga@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Daniel', 'Isaac', 'Kamanga', '+255 712 345 108', 'STUDENT', true, true, '2026-01-20 08:00:00', false),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567809', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'esther.mwakilanga@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Esther', 'Ruth', 'Mwakilanga', '+255 712 345 109', 'STUDENT', true, true, '2026-01-20 08:00:00', false),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567810', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'joseph.mwakasege@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Joseph', 'Daniel', 'Mwakasege', '+255 712 345 110', 'STUDENT', true, true, '2026-01-20 08:00:00', false);

-- 5. CREATE USERS (Parents)
INSERT INTO users (id, institution_id, email, password_hash, first_name, middle_name, last_name, phone, role, is_active, is_email_verified, created_at, is_deleted)
VALUES
  ('c1b2c3d4-e5f6-7890-abcd-ef1234567801', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'hassan.ali.parent@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Hassan', 'Mweve', 'Ali', '+255 712 345 201', 'PARENT', true, true, '2026-01-20 08:00:00', false),
  ('c1b2c3d4-e5f6-7890-abcd-ef1234567802', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'joseph.mushi.parent@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Joseph', 'Peter', 'Mushi', '+255 712 345 202', 'PARENT', true, true, '2026-01-20 08:00:00', false),
  ('c1b2c3d4-e5f6-7890-abcd-ef1234567803', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'elizabeth.ndunguru@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Elizabeth', 'Mary', 'Ndunguru', '+255 712 345 203', 'PARENT', true, true, '2026-01-20 08:00:00', false);

-- 6. CREATE INSTITUTION MEMBERSHIPS
INSERT INTO institution_memberships (user_id, institution_id, role, is_active, created_at)
VALUES
  ('a1b2c3d4-e5f6-7890-abcd-ef1234567801', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'TEACHER', true, '2026-01-15 08:00:00'),
  ('a1b2c3d4-e5f6-7890-abcd-ef1234567802', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'TEACHER', true, '2026-01-15 08:00:00'),
  ('a1b2c3d4-e5f6-7890-abcd-ef1234567803', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'TEACHER', true, '2026-02-01 08:00:00'),
  ('a1b2c3d4-e5f6-7890-abcd-ef1234567804', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'TEACHER', true, '2026-02-01 08:00:00'),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567801', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'STUDENT', true, '2026-01-20 08:00:00'),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567802', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'STUDENT', true, '2026-01-20 08:00:00'),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567803', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'STUDENT', true, '2026-01-20 08:00:00'),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567804', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'STUDENT', true, '2026-01-20 08:00:00'),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567805', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'STUDENT', true, '2026-01-20 08:00:00'),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567806', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'STUDENT', true, '2026-01-20 08:00:00'),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567807', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'STUDENT', true, '2026-01-20 08:00:00'),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567808', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'STUDENT', true, '2026-01-20 08:00:00'),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567809', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'STUDENT', true, '2026-01-20 08:00:00'),
  ('b1b2c3d4-e5f6-7890-abcd-ef1234567810', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'STUDENT', true, '2026-01-20 08:00:00'),
  ('c1b2c3d4-e5f6-7890-abcd-ef1234567801', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PARENT', true, '2026-01-20 08:00:00'),
  ('c1b2c3d4-e5f6-7890-abcd-ef1234567802', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PARENT', true, '2026-01-20 08:00:00'),
  ('c1b2c3d4-e5f6-7890-abcd-ef1234567803', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PARENT', true, '2026-01-20 08:00:00');

-- 7. CREATE ACADEMIC YEARS
INSERT INTO academic_years (id, institution_id, education_level, year_label, start_date, end_date, is_current, is_active, created_at, is_deleted)
VALUES
  ('d1a1c3d4-e5f6-7890-abcd-ef1234567801', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', '2026', '2026-01-10', '2026-12-15', true, true, '2026-01-10 08:00:00', false);

-- 8. CREATE TERMS
INSERT INTO terms (id, institution_id, academic_year_id, name, term_number, start_date, end_date, is_active, created_at, is_deleted)
VALUES
  ('e1a1c3d4-e5f6-7890-abcd-ef1234567801', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'Term 1', 1, '2026-01-10', '2026-04-10', true, '2026-01-10 08:00:00', false),
  ('e1a1c3d4-e5f6-7890-abcd-ef1234567802', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'Term 2', 2, '2026-04-20', '2026-07-20', true, '2026-01-10 08:00:00', false),
  ('e1a1c3d4-e5f6-7890-abcd-ef1234567803', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'Term 3', 3, '2026-08-01', '2026-12-15', true, '2026-01-10 08:00:00', false);

-- 9. CREATE GRADES
INSERT INTO grades (id, institution_id, education_level, name, code, sort_order, is_active, created_at, is_deleted)
VALUES
  ('f1a1c3d4-e5f6-7890-abcd-ef1234567801', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Standard 1', 'STD1', 1, true, '2026-01-10 08:00:00', false),
  ('f1a1c3d4-e5f6-7890-abcd-ef1234567802', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Standard 2', 'STD2', 2, true, '2026-01-10 08:00:00', false),
  ('f1a1c3d4-e5f6-7890-abcd-ef1234567803', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Standard 3', 'STD3', 3, true, '2026-01-10 08:00:00', false),
  ('f1a1c3d4-e5f6-7890-abcd-ef1234567804', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Standard 4', 'STD4', 4, true, '2026-01-10 08:00:00', false),
  ('f1a1c3d4-e5f6-7890-abcd-ef1234567805', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Standard 5', 'STD5', 5, true, '2026-01-10 08:00:00', false),
  ('f1a1c3d4-e5f6-7890-abcd-ef1234567806', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Standard 6', 'STD6', 6, true, '2026-01-10 08:00:00', false),
  ('f1a1c3d4-e5f6-7890-abcd-ef1234567807', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Standard 7', 'STD7', 7, true, '2026-01-10 08:00:00', false);

-- 10. SUBJECTS
INSERT INTO subjects (id, institution_id, education_level, name, code, description, is_active, created_at, is_deleted)
VALUES
  ('a2000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Mathematics', 'MATH', 'Arithmetic, algebra, geometry, and problem solving', true, '2026-01-10 08:00:00', false),
  ('a2000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'English Language', 'ENG', 'Reading, writing, grammar, and comprehension', true, '2026-01-10 08:00:00', false),
  ('a2000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Kiswahili', 'KIS', 'Kiswahili language and literature', true, '2026-01-10 08:00:00', false),
  ('a2000000-0000-0000-0000-000000000004', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Science and Technology', 'SCI', 'Basic science, biology, physics, chemistry concepts', true, '2026-01-10 08:00:00', false),
  ('a2000000-0000-0000-0000-000000000005', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Social Studies', 'SST', 'Geography, history, and civic education', true, '2026-01-10 08:00:00', false),
  ('a2000000-0000-0000-0000-000000000006', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Religious Education', 'RE', 'Moral and religious studies', true, '2026-01-10 08:00:00', false),
  ('a2000000-0000-0000-0000-000000000007', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Physical Education', 'PE', 'Sports, health, and fitness', true, '2026-01-10 08:00:00', false),
  ('a2000000-0000-0000-0000-000000000008', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'PRIMARY', 'Art and Craft', 'ART', 'Creative arts, drawing, and crafts', true, '2026-01-10 08:00:00', false);

-- 11. CLASSES
INSERT INTO classes (id, institution_id, name, code, level, section, capacity, academic_year, is_active, created_at, is_deleted)
VALUES
  ('a3000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'Standard 3 - A', 'STD3A', 'PRIMARY', 'A', 40, '2026', true, '2026-01-10 08:00:00', false),
  ('a3000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'Standard 4 - A', 'STD4A', 'PRIMARY', 'A', 40, '2026', true, '2026-01-10 08:00:00', false),
  ('a3000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'Standard 5 - A', 'STD5A', 'PRIMARY', 'A', 40, '2026', true, '2026-01-10 08:00:00', false);

-- 12. TEACHERS
INSERT INTO teachers (id, institution_id, user_id, employee_number, status, specialization, hire_date, bio, created_at, is_deleted)
VALUES
  ('a4000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a1b2c3d4-e5f6-7890-abcd-ef1234567801', 'TCH-001', 'ACTIVE', 'Mathematics', '2026-01-15', 'Experienced mathematics teacher with 8 years of teaching experience', '2026-01-15 08:00:00', false),
  ('a4000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a1b2c3d4-e5f6-7890-abcd-ef1234567802', 'TCH-002', 'ACTIVE', 'English Language', '2026-01-15', 'English language specialist with passion for literature', '2026-01-15 08:00:00', false),
  ('a4000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a1b2c3d4-e5f6-7890-abcd-ef1234567803', 'TCH-003', 'ACTIVE', 'Science', '2026-02-01', 'Science teacher specializing in biology and environmental science', '2026-02-01 08:00:00', false),
  ('a4000000-0000-0000-0000-000000000004', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a1b2c3d4-e5f6-7890-abcd-ef1234567804', 'TCH-004', 'ACTIVE', 'Kiswahili', '2026-02-01', 'Kiswahili language and cultural studies expert', '2026-02-01 08:00:00', false);

-- 13. STUDENTS
INSERT INTO students (id, institution_id, user_id, admission_number, status, date_of_birth, gender, address, city, region, guardian_name, guardian_phone, guardian_email, guardian_relationship, enrollment_date, created_at, is_deleted)
VALUES
  ('a5000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b1b2c3d4-e5f6-7890-abcd-ef1234567801', 'STU-2026-002', 'ACTIVE', '2016-03-15', 'FEMALE', '456 Mwananyamala Rd', 'Dar es Salaam', 'Dar es Salaam', 'Hassan Ali', '+255 712 345 201', 'hassan.ali.parent@test.com', 'Father', '2026-01-20', '2026-01-20 08:00:00', false),
  ('a5000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b1b2c3d4-e5f6-7890-abcd-ef1234567802', 'STU-2026-003', 'ACTIVE', '2015-07-22', 'MALE', '789 Sinza', 'Dar es Salaam', 'Dar es Salaam', 'Joseph Mushi', '+255 712 345 202', 'joseph.mushi.parent@test.com', 'Father', '2026-01-20', '2026-01-20 08:00:00', false),
  ('a5000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b1b2c3d4-e5f6-7890-abcd-ef1234567803', 'STU-2026-004', 'ACTIVE', '2016-11-08', 'FEMALE', '321 Mikocheni', 'Dar es Salaam', 'Dar es Salaam', 'Ibrahim Omar', '+255 712 345 103', 'ibrahim.omar@test.com', 'Father', '2026-01-20', '2026-01-20 08:00:00', false),
  ('a5000000-0000-0000-0000-000000000004', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b1b2c3d4-e5f6-7890-abcd-ef1234567804', 'STU-2026-005', 'ACTIVE', '2015-05-30', 'MALE', '654 Kinondoni', 'Dar es Salaam', 'Dar es Salaam', 'Elizabeth Ndunguru', '+255 712 345 203', 'elizabeth.ndunguru@test.com', 'Mother', '2026-01-20', '2026-01-20 08:00:00', false),
  ('a5000000-0000-0000-0000-000000000005', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b1b2c3d4-e5f6-7890-abcd-ef1234567805', 'STU-2026-006', 'ACTIVE', '2016-09-12', 'FEMALE', '987 Mbezi', 'Dar es Salaam', 'Dar es Salaam', 'Samuel Kilanga', '+255 712 345 105', 'samuel.kilanga@test.com', 'Father', '2026-01-20', '2026-01-20 08:00:00', false),
  ('a5000000-0000-0000-0000-000000000006', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b1b2c3d4-e5f6-7890-abcd-ef1234567806', 'STU-2026-007', 'ACTIVE', '2015-12-01', 'MALE', '147 Tandale', 'Dar es Salaam', 'Dar es Salaam', 'Ali Mwinyi', '+255 712 345 106', 'ali.mwinyi@test.com', 'Father', '2026-01-20', '2026-01-20 08:00:00', false),
  ('a5000000-0000-0000-0000-000000000007', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b1b2c3d4-e5f6-7890-abcd-ef1234567807', 'STU-2026-008', 'ACTIVE', '2016-04-18', 'FEMALE', '258 Buguruni', 'Dar es Salaam', 'Dar es Salaam', 'Joyce Mwamba', '+255 712 345 107', 'joyce.mwamba@test.com', 'Mother', '2026-01-20', '2026-01-20 08:00:00', false),
  ('a5000000-0000-0000-0000-000000000008', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b1b2c3d4-e5f6-7890-abcd-ef1234567808', 'STU-2026-009', 'ACTIVE', '2015-08-25', 'MALE', '369 Vingunguti', 'Dar es Salaam', 'Dar es Salaam', 'Isaac Kamanga', '+255 712 345 108', 'isaac.kamanga@test.com', 'Father', '2026-01-20', '2026-01-20 08:00:00', false),
  ('a5000000-0000-0000-0000-000000000009', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b1b2c3d4-e5f6-7890-abcd-ef1234567809', 'STU-2026-010', 'ACTIVE', '2016-02-14', 'FEMALE', '741 Magomeni', 'Dar es Salaam', 'Dar es Salaam', 'Ruth Mwakilanga', '+255 712 345 109', 'ruth.mwakilanga@test.com', 'Mother', '2026-01-20', '2026-01-20 08:00:00', false),
  ('a5000000-0000-0000-0000-000000000010', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b1b2c3d4-e5f6-7890-abcd-ef1234567810', 'STU-2026-011', 'ACTIVE', '2015-10-07', 'MALE', '852 Kigogo', 'Dar es Salaam', 'Dar es Salaam', 'Daniel Mwakasege', '+255 712 345 110', 'daniel.mwakasege@test.com', 'Father', '2026-01-20', '2026-01-20 08:00:00', false);

-- 14. ENROLLMENTS (class_group_id -> classes.id, per V10 FK)
INSERT INTO enrollments (id, institution_id, student_id, class_group_id, academic_year_id, status, enrolled_at, created_at, is_deleted)
VALUES
  ('a6000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000001', 'a3000000-0000-0000-0000-000000000001', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'ENROLLED', '2026-01-20 08:00:00', '2026-01-20 08:00:00', false),
  ('a6000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000001', 'a3000000-0000-0000-0000-000000000001', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'ENROLLED', '2026-01-20 08:00:00', '2026-01-20 08:00:00', false),
  ('a6000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000002', 'a3000000-0000-0000-0000-000000000001', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'ENROLLED', '2026-01-20 08:00:00', '2026-01-20 08:00:00', false),
  ('a6000000-0000-0000-0000-000000000004', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000001', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'ENROLLED', '2026-01-20 08:00:00', '2026-01-20 08:00:00', false),
  ('a6000000-0000-0000-0000-000000000005', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000004', 'a3000000-0000-0000-0000-000000000002', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'ENROLLED', '2026-01-20 08:00:00', '2026-01-20 08:00:00', false),
  ('a6000000-0000-0000-0000-000000000006', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000005', 'a3000000-0000-0000-0000-000000000002', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'ENROLLED', '2026-01-20 08:00:00', '2026-01-20 08:00:00', false),
  ('a6000000-0000-0000-0000-000000000007', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000006', 'a3000000-0000-0000-0000-000000000002', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'ENROLLED', '2026-01-20 08:00:00', '2026-01-20 08:00:00', false),
  ('a6000000-0000-0000-0000-000000000008', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000007', 'a3000000-0000-0000-0000-000000000003', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'ENROLLED', '2026-01-20 08:00:00', '2026-01-20 08:00:00', false),
  ('a6000000-0000-0000-0000-000000000009', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000008', 'a3000000-0000-0000-0000-000000000003', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'ENROLLED', '2026-01-20 08:00:00', '2026-01-20 08:00:00', false),
  ('a6000000-0000-0000-0000-000000000010', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000009', 'a3000000-0000-0000-0000-000000000003', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'ENROLLED', '2026-01-20 08:00:00', '2026-01-20 08:00:00', false),
  ('a6000000-0000-0000-0000-000000000011', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000010', 'a3000000-0000-0000-0000-000000000003', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'ENROLLED', '2026-01-20 08:00:00', '2026-01-20 08:00:00', false);

-- 15. TEACHER ASSIGNMENTS (class_group_id -> classes.id per V04 FK)
INSERT INTO teacher_assignments (id, institution_id, teacher_id, class_group_id, subject_id, academic_year, created_at, is_deleted)
VALUES
  ('a7000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a4000000-0000-0000-0000-000000000001', 'a3000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000001', '2026', '2026-01-15 08:00:00', false),
  ('a7000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a4000000-0000-0000-0000-000000000001', 'a3000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000001', '2026', '2026-01-15 08:00:00', false),
  ('a7000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a4000000-0000-0000-0000-000000000002', 'a3000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000002', '2026', '2026-01-15 08:00:00', false),
  ('a7000000-0000-0000-0000-000000000004', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a4000000-0000-0000-0000-000000000002', 'a3000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', '2026', '2026-01-15 08:00:00', false),
  ('a7000000-0000-0000-0000-000000000005', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a4000000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000004', '2026', '2026-02-01 08:00:00', false),
  ('a7000000-0000-0000-0000-000000000006', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a4000000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000004', '2026', '2026-02-01 08:00:00', false),
  ('a7000000-0000-0000-0000-000000000007', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a4000000-0000-0000-0000-000000000004', 'a3000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000003', '2026', '2026-02-01 08:00:00', false),
  ('a7000000-0000-0000-0000-000000000008', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a4000000-0000-0000-0000-000000000004', 'a3000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000003', '2026', '2026-02-01 08:00:00', false);

-- 16. COURSES (created_by is UUID per V08)
INSERT INTO courses (id, institution_id, subject_id, title, description, level, category, is_published, is_featured, created_by, created_at, is_deleted)
VALUES
  ('a8000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000001', 'Mathematics Standard 3', 'Complete mathematics course for Standard 3 students covering arithmetic, fractions, and basic geometry', 'PRIMARY', 'Mathematics', true, true, '22548351-f15a-4f60-b1ff-f807743ef4cc', '2026-01-15 08:00:00', false),
  ('a8000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000002', 'English Language Standard 3', 'English reading, writing, and comprehension for Standard 3', 'PRIMARY', 'English', true, false, '22548351-f15a-4f60-b1ff-f807743ef4cc', '2026-01-15 08:00:00', false),
  ('a8000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000004', 'Science and Technology Standard 4', 'Introduction to science concepts for Standard 4 students', 'PRIMARY', 'Science', true, true, '22548351-f15a-4f60-b1ff-f807743ef4cc', '2026-02-01 08:00:00', false),
  ('a8000000-0000-0000-0000-000000000004', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000005', 'Social Studies Standard 5', 'Tanzanian history, geography, and civic education', 'PRIMARY', 'Social Studies', true, false, '22548351-f15a-4f60-b1ff-f807743ef4cc', '2026-02-01 08:00:00', false),
  ('a8000000-0000-0000-0000-000000000005', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000003', 'Kiswahili Standard 3', 'Kiswahili language and literature for Standard 3', 'PRIMARY', 'Kiswahili', true, false, '22548351-f15a-4f60-b1ff-f807743ef4cc', '2026-02-01 08:00:00', false);

-- 17. COURSE MODULES (V08: no institution_id column)
INSERT INTO course_modules (id, course_id, title, description, sort_order, created_at, is_deleted)
VALUES
  ('a9000000-0000-0000-0000-000000000001', 'a8000000-0000-0000-0000-000000000001', 'Number Operations', 'Addition, subtraction, multiplication and division', 1, '2026-01-15 08:00:00', false),
  ('a9000000-0000-0000-0000-000000000002', 'a8000000-0000-0000-0000-000000000001', 'Fractions', 'Understanding and working with fractions', 2, '2026-01-15 08:00:00', false),
  ('a9000000-0000-0000-0000-000000000003', 'a8000000-0000-0000-0000-000000000001', 'Basic Geometry', 'Shapes, sizes, and spatial relationships', 3, '2026-01-15 08:00:00', false),
  ('a9000000-0000-0000-0000-000000000004', 'a8000000-0000-0000-0000-000000000002', 'Reading Comprehension', 'Understanding texts and answering questions', 1, '2026-01-15 08:00:00', false),
  ('a9000000-0000-0000-0000-000000000005', 'a8000000-0000-0000-0000-000000000002', 'Grammar and Writing', 'Sentence structure and paragraph writing', 2, '2026-01-15 08:00:00', false),
  ('a9000000-0000-0000-0000-000000000006', 'a8000000-0000-0000-0000-000000000003', 'Living Things', 'Plants, animals, and their habitats', 1, '2026-02-01 08:00:00', false),
  ('a9000000-0000-0000-0000-000000000007', 'a8000000-0000-0000-0000-000000000003', 'Materials and Properties', 'Solids, liquids, and gases', 2, '2026-02-01 08:00:00', false);

-- 18. COURSE LESSONS (V08: no institution_id column)
INSERT INTO course_lessons (id, module_id, title, content_type, content_url, duration_minutes, sort_order, is_free, created_at, is_deleted)
VALUES
  ('b0000000-0000-0000-0000-000000000001', 'a9000000-0000-0000-0000-000000000001', 'Addition and Subtraction', 'VIDEO', 'https://example.com/math-add-sub', 20, 1, true, '2026-01-15 08:00:00', false),
  ('b0000000-0000-0000-0000-000000000002', 'a9000000-0000-0000-0000-000000000001', 'Multiplication Tables', 'VIDEO', 'https://example.com/math-multiply', 15, 2, false, '2026-01-15 08:00:00', false),
  ('b0000000-0000-0000-0000-000000000003', 'a9000000-0000-0000-0000-000000000001', 'Division Practice', 'DOCUMENT', 'https://example.com/math-div.pdf', 25, 3, false, '2026-01-15 08:00:00', false),
  ('b0000000-0000-0000-0000-000000000004', 'a9000000-0000-0000-0000-000000000002', 'Understanding Halves and Quarters', 'VIDEO', 'https://example.com/math-fractions-1', 18, 1, true, '2026-01-15 08:00:00', false),
  ('b0000000-0000-0000-0000-000000000005', 'a9000000-0000-0000-0000-000000000002', 'Comparing Fractions', 'DOCUMENT', 'https://example.com/math-fractions-2.pdf', 22, 2, false, '2026-01-15 08:00:00', false),
  ('b0000000-0000-0000-0000-000000000006', 'a9000000-0000-0000-0000-000000000003', '2D and 3D Shapes', 'VIDEO', 'https://example.com/math-shapes', 20, 1, true, '2026-01-15 08:00:00', false),
  ('b0000000-0000-0000-0000-000000000007', 'a9000000-0000-0000-0000-000000000004', 'Reading a Story', 'VIDEO', 'https://example.com/eng-read-1', 20, 1, true, '2026-01-15 08:00:00', false),
  ('b0000000-0000-0000-0000-000000000008', 'a9000000-0000-0000-0000-000000000004', 'Answering Questions', 'DOCUMENT', 'https://example.com/eng-read-2.pdf', 15, 2, false, '2026-01-15 08:00:00', false),
  ('b0000000-0000-0000-0000-000000000009', 'a9000000-0000-0000-0000-000000000005', 'Parts of Speech', 'VIDEO', 'https://example.com/eng-grammar', 20, 1, true, '2026-01-15 08:00:00', false),
  ('b0000000-0000-0000-0000-000000000010', 'a9000000-0000-0000-0000-000000000006', 'Plants Around Us', 'VIDEO', 'https://example.com/sci-plants', 18, 1, true, '2026-02-01 08:00:00', false),
  ('b0000000-0000-0000-0000-000000000011', 'a9000000-0000-0000-0000-000000000006', 'Animal Habitats', 'DOCUMENT', 'https://example.com/sci-animals.pdf', 22, 2, false, '2026-02-01 08:00:00', false);

-- 19. ASSIGNMENTS
INSERT INTO assignments (id, institution_id, subject_id, class_group_id, title, description, due_date, total_marks, created_at, is_deleted)
VALUES
  ('b1000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000001', 'a3000000-0000-0000-0000-000000000001', 'Addition Worksheet', 'Complete exercises on addition of 2-digit numbers', '2026-09-15', 50, '2026-09-01 08:00:00', false),
  ('b1000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000002', 'a3000000-0000-0000-0000-000000000001', 'Essay Writing', 'Write a 200-word essay about your best friend', '2026-09-18', 100, '2026-09-01 08:00:00', false),
  ('b1000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000004', 'a3000000-0000-0000-0000-000000000002', 'Science Project', 'Create a poster about the water cycle', '2026-09-20', 100, '2026-09-05 08:00:00', false);

-- 20. ASSESSMENTS
INSERT INTO assessments (id, institution_id, subject_id, class_group_id, title, description, time_limit_minutes, total_marks, pass_marks, is_published, starts_at, ends_at, created_at, is_deleted)
VALUES
  ('b2000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000001', 'a3000000-0000-0000-0000-000000000001', 'Mid-Term Math Test', 'Test on addition, subtraction, and multiplication', 45, 100, 50, true, '2026-09-10 09:00:00', '2026-09-10 09:45:00', '2026-09-01 08:00:00', false),
  ('b2000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000002', 'a3000000-0000-0000-0000-000000000001', 'English Comprehension Test', 'Reading comprehension and grammar', 30, 50, 25, true, '2026-09-12 10:00:00', '2026-09-12 10:30:00', '2026-09-01 08:00:00', false),
  ('b2000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000004', 'a3000000-0000-0000-0000-000000000002', 'Science Quiz', 'Quiz on plants and animals', 20, 40, 20, true, '2026-09-14 11:00:00', '2026-09-14 11:20:00', '2026-09-05 08:00:00', false);

-- 21. GRADING SCALES (V13: no is_active, no scale_type)
INSERT INTO grading_scales (id, institution_id, name, description, is_default, created_at, is_deleted)
VALUES
  ('b5000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'Primary School Scale', 'Standard grading scale for primary schools', true, '2026-01-10 08:00:00', false);

-- 22. GRADE BOUNDARIES (V13: no grade_label, no grade_name, no sort_order)
INSERT INTO grade_boundaries (id, institution_id, grading_scale_id, grade_letter, min_percentage, max_percentage, description, gpa_points, created_at, is_deleted)
VALUES
  ('b6000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b5000000-0000-0000-0000-000000000001', 'A+', 90, 100, 'Outstanding', 4.0, '2026-01-10 08:00:00', false),
  ('b6000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b5000000-0000-0000-0000-000000000001', 'A', 80, 89.99, 'Excellent', 4.0, '2026-01-10 08:00:00', false),
  ('b6000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b5000000-0000-0000-0000-000000000001', 'B+', 75, 79.99, 'Very Good', 3.5, '2026-01-10 08:00:00', false),
  ('b6000000-0000-0000-0000-000000000004', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b5000000-0000-0000-0000-000000000001', 'B', 70, 74.99, 'Good', 3.0, '2026-01-10 08:00:00', false),
  ('b6000000-0000-0000-0000-000000000005', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b5000000-0000-0000-0000-000000000001', 'C', 60, 69.99, 'Average', 2.0, '2026-01-10 08:00:00', false),
  ('b6000000-0000-0000-0000-000000000006', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b5000000-0000-0000-0000-000000000001', 'D', 50, 59.99, 'Below Average', 1.0, '2026-01-10 08:00:00', false),
  ('b6000000-0000-0000-0000-000000000007', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b5000000-0000-0000-0000-000000000001', 'F', 0, 49.99, 'Failed', 0.0, '2026-01-10 08:00:00', false);

-- 23. REPORT CARDS (V13: no grading_scale_id, no status)
INSERT INTO report_cards (id, institution_id, student_id, class_group_id, academic_year_id, term_id, total_marks, average_marks, overall_grade, rank_in_class, remarks, generated_at, created_at, is_deleted)
VALUES
  ('b3000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000001', 'a3000000-0000-0000-0000-000000000001', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'e1a1c3d4-e5f6-7890-abcd-ef1234567801', 450, 78.5, 'B+', 3, 'Good performance, keep it up!', '2026-04-10 10:00:00', '2026-04-10 10:00:00', false),
  ('b3000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000001', 'a3000000-0000-0000-0000-000000000001', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'e1a1c3d4-e5f6-7890-abcd-ef1234567801', 480, 85.2, 'A', 1, 'Excellent performance!', '2026-04-10 10:00:00', '2026-04-10 10:00:00', false),
  ('b3000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000002', 'a3000000-0000-0000-0000-000000000001', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'e1a1c3d4-e5f6-7890-abcd-ef1234567801', 420, 72.0, 'B', 5, 'Good effort, room for improvement', '2026-04-10 10:00:00', '2026-04-10 10:00:00', false),
  ('b3000000-0000-0000-0000-000000000004', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000001', 'd1a1c3d4-e5f6-7890-abcd-ef1234567801', 'e1a1c3d4-e5f6-7890-abcd-ef1234567801', 465, 81.3, 'A-', 2, 'Very good performance!', '2026-04-10 10:00:00', '2026-04-10 10:00:00', false);

-- 24. SUBJECT GRADES (V13: no grade_points, no assessed_by, no assessed_at)
INSERT INTO subject_grades (id, institution_id, report_card_id, subject_id, marks_obtained, grade, comments, created_at, is_deleted)
VALUES
  ('b4000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b3000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000001', 85, 'A', 'Excellent understanding of math concepts', '2026-04-08 10:00:00', false),
  ('b4000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b3000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000002', 78, 'B+', 'Good writing skills, work on vocabulary', '2026-04-08 10:00:00', false),
  ('b4000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b3000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000003', 82, 'A-', 'Very good Kiswahili skills', '2026-04-08 10:00:00', false),
  ('b4000000-0000-0000-0000-000000000004', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b3000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000004', 70, 'B', 'Good science understanding', '2026-04-08 10:00:00', false),
  ('b4000000-0000-0000-0000-000000000005', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b3000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000001', 92, 'A+', 'Outstanding mathematical ability', '2026-04-08 10:00:00', false),
  ('b4000000-0000-0000-0000-000000000006', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b3000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 88, 'A', 'Excellent English skills', '2026-04-08 10:00:00', false),
  ('b4000000-0000-0000-0000-000000000007', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b3000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000003', 80, 'A-', 'Good Kiswahili performance', '2026-04-08 10:00:00', false),
  ('b4000000-0000-0000-0000-000000000008', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b3000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000004', 81, 'A-', 'Excellent science projects', '2026-04-08 10:00:00', false);

-- 25. ATTENDANCE RECORDS (V14: no attendance_date, only record_date)
INSERT INTO attendance_records (institution_id, student_id, class_group_id, record_date, status, remarks, marked_by, created_at, is_deleted)
SELECT 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', s.id, 'a3000000-0000-0000-0000-000000000001', dt,
  CASE WHEN random() > 0.1 THEN 'PRESENT' ELSE 'ABSENT' END,
  CASE WHEN random() > 0.1 THEN NULL ELSE 'Absent without permission' END,
  'a4000000-0000-0000-0000-000000000001', NOW(), false
FROM (SELECT '2026-09-01'::date AS dt UNION ALL SELECT '2026-09-02' UNION ALL SELECT '2026-09-03' UNION ALL SELECT '2026-09-04' UNION ALL SELECT '2026-09-05' UNION ALL SELECT '2026-09-08' UNION ALL SELECT '2026-09-09') dates
CROSS JOIN (SELECT id FROM students WHERE institution_id = 'fbd2e3e3-99df-48f3-b138-58d6f6f84103' LIMIT 4) s;

-- 26. PARENTS
INSERT INTO parents (id, institution_id, user_id, relationship_type, occupation, created_at, is_deleted)
VALUES
  ('b7000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'c1b2c3d4-e5f6-7890-abcd-ef1234567801', 'GUARDIAN', 'Business', '2026-01-20 08:00:00', false),
  ('b7000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'c1b2c3d4-e5f6-7890-abcd-ef1234567802', 'GUARDIAN', 'Teacher', '2026-01-20 08:00:00', false),
  ('b7000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'c1b2c3d4-e5f6-7890-abcd-ef1234567803', 'GUARDIAN', 'Nurse', '2026-01-20 08:00:00', false);

-- 27. PARENT-STUDENT LINKS
INSERT INTO parent_student_links (id, institution_id, parent_id, student_id, relationship_type, is_primary, created_at, is_deleted)
VALUES
  ('b8000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b7000000-0000-0000-0000-000000000001', 'a5000000-0000-0000-0000-000000000001', 'GUARDIAN', true, '2026-01-20 08:00:00', false),
  ('b8000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b7000000-0000-0000-0000-000000000002', 'a5000000-0000-0000-0000-000000000002', 'GUARDIAN', true, '2026-01-20 08:00:00', false),
  ('b8000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b7000000-0000-0000-0000-000000000003', 'a5000000-0000-0000-0000-000000000004', 'GUARDIAN', true, '2026-01-20 08:00:00', false);

-- 28. LIVE CLASSES
INSERT INTO live_classes (id, institution_id, subject_id, teacher_id, title, description, scheduled_at, duration_minutes, status, meeting_url, max_participants, created_at, is_deleted)
VALUES
  ('b9000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000001', 'a4000000-0000-0000-0000-000000000001', 'Mathematics Review Session', 'Review of fractions and basic geometry', '2026-09-12 10:00:00', 60, 'SCHEDULED', 'https://meet.example.com/math-review', 40, '2026-09-09 08:00:00', false),
  ('b9000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000002', 'a4000000-0000-0000-0000-000000000002', 'English Reading Practice', 'Interactive reading and comprehension session', '2026-09-13 14:00:00', 45, 'SCHEDULED', 'https://meet.example.com/eng-read', 40, '2026-09-09 08:00:00', false),
  ('b9000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a2000000-0000-0000-0000-000000000004', 'a4000000-0000-0000-0000-000000000003', 'Science Experiment Demo', 'Live demonstration of plant growth experiment', '2026-09-15 11:00:00', 30, 'COMPLETED', 'https://meet.example.com/sci-demo', 35, '2026-09-05 08:00:00', false);

-- 29. ACTIVITY FEEDS (V18: no action, no actor_name, no entity_name, no visibility)
INSERT INTO activity_feeds (id, institution_id, user_id, activity_type, title, description, entity_type, entity_id, is_read, created_at, is_deleted)
VALUES
  ('c0000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', '22548351-f15a-4f60-b1ff-f807743ef4cc', 'COURSE_CREATED', 'New Course Created', 'Mathematics Standard 3 course was created', 'Course', 'a8000000-0000-0000-0000-000000000001', false, '2026-01-15 08:00:00', false),
  ('c0000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', '22548351-f15a-4f60-b1ff-f807743ef4cc', 'CERTIFICATE_ISSUED', 'Certificate Issued', 'Certificate of Completion issued to Test Student', 'Certificate', NULL, false, '2026-09-09 21:34:08', false),
  ('c0000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a1b2c3d4-e5f6-7890-abcd-ef1234567801', 'TEACHER_JOINED', 'Teacher Joined', 'Mary Johnson joined as Mathematics teacher', 'Teacher', 'a4000000-0000-0000-0000-000000000001', false, '2026-01-15 08:00:00', false);

-- 30. SECURITY EVENTS (V18: no user_email)
INSERT INTO security_events (id, institution_id, user_id, event_type, description, severity, ip_address, user_agent, resolved, created_at, is_deleted)
VALUES
  ('c1000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', '22548351-f15a-4f60-b1ff-f807743ef4cc', 'LOGIN_SUCCESS', 'Admin logged in successfully', 'INFO', '192.168.1.100', 'Mozilla/5.0', true, '2026-09-09 08:00:00', false),
  ('c1000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'b1b2c3d4-e5f6-7890-abcd-ef1234567801', 'LOGIN_SUCCESS', 'Student logged in successfully', 'INFO', '192.168.1.101', 'Mozilla/5.0', true, '2026-09-09 08:05:00', false);

-- 31. CERTIFICATES (V16 schema: id, institution_id, student_id, template_id, certificate_number, title, description, issued_date, expiry_date, status, verification_code, pdf_url, revoked_at, revocation_reason, + audit columns)
INSERT INTO certificates (id, institution_id, student_id, template_id, certificate_number, title, description, issued_date, expiry_date, status, verification_code, created_by, is_deleted)
VALUES
  ('c2000000-0000-0000-0000-000000000001', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000001', NULL, 'CERT-ACH-2026-000001', 'Mathematics Excellence Award', 'Awarded for outstanding performance in Mathematics', '2026-04-10', NULL, 'ACTIVE', 'ACH2026001ABCDEF', '22548351-f15a-4f60-b1ff-f807743ef4cc', false),
  ('c2000000-0000-0000-0000-000000000002', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000002', NULL, 'CERT-PAR-2026-000001', 'Sports Day Certificate', 'Participated in inter-school athletics competition', '2026-03-15', NULL, 'ACTIVE', 'PAR2026001ABCDEF', '22548351-f15a-4f60-b1ff-f807743ef4cc', false),
  ('c2000000-0000-0000-0000-000000000003', 'fbd2e3e3-99df-48f3-b138-58d6f6f84103', 'a5000000-0000-0000-0000-000000000001', NULL, 'CERT-ACH-2026-000002', 'Science Fair Winner', 'First place in school science fair 2026', '2026-03-20', NULL, 'ACTIVE', 'ACH2026002ABCDEF', '22548351-f15a-4f60-b1ff-f807743ef4cc', false);
