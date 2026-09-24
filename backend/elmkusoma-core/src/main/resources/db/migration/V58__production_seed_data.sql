-- V58: Production seed data for ELMKUSOMA platform
-- This migration seeds essential data for production deployment

-- 1. Core Institutions
INSERT INTO institutions (id, name, code, address, phone, email, website, type, is_active, is_deleted, created_at, updated_at)
VALUES
  ('a0000000-0000-0000-0000-000000000001', 'ELMKUSOMA National HQ', 'ELMKUSOMA-HQ', 'Dodoma, Tanzania', '+255-22-2110001', 'admin@elmkusoma.go.tz', 'https://elmkusoma.go.tz', 'NATIONAL', true, false, NOW(), NOW()),
  ('a0000000-0000-0000-0000-000000000002', 'Dar es Salaam Model School', 'DMS-001', 'Dar es Salaam, Tanzania', '+255-22-2110002', 'admin@darms.edu.tz', NULL, 'SCHOOL', true, false, NOW(), NOW()),
  ('a0000000-0000-0000-0000-000000000003', 'Arusha Technical College', 'ATC-001', 'Arusha, Tanzania', '+255-22-2110003', 'admin@arushatc.edu.tz', NULL, 'COLLEGE', true, false, NOW(), NOW()),
  ('a0000000-0000-0000-0000-000000000004', 'Mwanza Primary Academy', 'MPA-001', 'Mwanza, Tanzania', '+255-22-2110004', 'admin@mwanza.edu.tz', NULL, 'SCHOOL', true, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. Admin Users (passwords are BCrypt encoded for 'password')
INSERT INTO users (id, institution_id, email, password_hash, first_name, last_name, role, is_active, is_email_verified, is_deleted, created_at, updated_at)
VALUES
  ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'national@elmkusoma.go.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'National', 'Admin', 'NATIONAL_ADMIN', true, true, false, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'regional@elmkusoma.go.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Regional', 'Admin', 'REGIONAL_ADMIN', true, true, false, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'district@elmkusoma.go.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'District', 'Admin', 'DISTRICT_ADMIN', true, true, false, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000002', 'admin@darms.edu.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'School', 'Admin', 'INSTITUTION_ADMIN', true, true, false, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000003', 'admin@arushatc.edu.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'College', 'Admin', 'INSTITUTION_ADMIN', true, true, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 3. Teacher Users
INSERT INTO users (id, institution_id, email, password_hash, first_name, last_name, role, is_active, is_email_verified, is_deleted, created_at, updated_at)
VALUES
  ('b0000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000002', 'teacher1@darms.edu.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Amina', 'Mwangi', 'TEACHER', true, true, false, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000002', 'teacher2@darms.edu.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'John', 'Kimaro', 'TEACHER', true, true, false, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000012', 'a0000000-0000-0000-0000-000000000003', 'teacher1@arushatc.edu.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Grace', 'Mushi', 'INSTRUCTOR', true, true, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 4. Student/Learner Users
INSERT INTO users (id, institution_id, email, password_hash, first_name, last_name, role, is_active, is_email_verified, is_deleted, created_at, updated_at)
VALUES
  ('b0000000-0000-0000-0000-000000000020', 'a0000000-0000-0000-0000-000000000002', 'student1@darms.edu.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Hassan', 'Omar', 'STUDENT', true, true, false, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000021', 'a0000000-0000-0000-0000-000000000002', 'student2@darms.edu.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Fatima', 'Juma', 'STUDENT', true, true, false, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000022', 'a0000000-0000-0000-0000-000000000002', 'learner1@darms.edu.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Peter', 'Makamba', 'OTHER_LEARNER', true, true, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 5. Parent Users
INSERT INTO users (id, institution_id, email, password_hash, first_name, last_name, role, is_active, is_email_verified, is_deleted, created_at, updated_at)
VALUES
  ('b0000000-0000-0000-0000-000000000030', 'a0000000-0000-0000-0000-000000000002', 'parent1@darms.edu.tz', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Omar', 'Hassan', 'PARENT', true, true, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 6. Teachers table entries (linking to users)
INSERT INTO teachers (id, user_id, institution_id, employee_number, specialization, hire_date, is_deleted, created_at, updated_at)
VALUES
  ('c0000000-0000-0000-0000-000000000010', 'b0000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000002', 'TCH-DMS-001', 'Mathematics', CURRENT_DATE, false, NOW(), NOW()),
  ('c0000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000002', 'TCH-DMS-002', 'Science', CURRENT_DATE, false, NOW(), NOW()),
  ('c0000000-0000-0000-0000-000000000012', 'b0000000-0000-0000-0000-000000000012', 'a0000000-0000-0000-0000-000000000003', 'TCH-ATC-001', 'Engineering', CURRENT_DATE, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 7. Academic Subjects
INSERT INTO subjects (id, institution_id, name, code, description, education_level, is_active, is_deleted, created_at, updated_at)
VALUES
  ('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'Mathematics', 'MATH', 'Core Mathematics curriculum', 'O_LEVEL', true, false, NOW(), NOW()),
  ('d0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'Science', 'SCI', 'General Science', 'O_LEVEL', true, false, NOW(), NOW()),
  ('d0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000002', 'English', 'ENG', 'English Language and Literature', 'O_LEVEL', true, false, NOW(), NOW()),
  ('d0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000002', 'Kiswahili', 'KIS', 'Kiswahili Language', 'O_LEVEL', true, false, NOW(), NOW()),
  ('d0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000003', 'Computer Science', 'CS', 'Computer Science and IT', 'A_LEVEL', true, false, NOW(), NOW()),
  ('d0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000004', 'Basic Mathematics', 'BMATH', 'Primary school mathematics', 'O_LEVEL', true, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 8. Sample Courses
INSERT INTO courses (id, institution_id, title, description, level, category, thumbnail_url, is_published, is_featured, is_deleted, created_at, updated_at)
VALUES
  ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'Introduction to Algebra', 'Learn the fundamentals of algebraic expressions, equations, and functions.', 'BEGINNER', 'Mathematics', NULL, true, true, false, NOW(), NOW()),
  ('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'Physics for O-Level', 'Comprehensive physics course covering mechanics, electricity, and waves.', 'INTERMEDIATE', 'Science', NULL, true, true, false, NOW(), NOW()),
  ('e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'Web Development Fundamentals', 'Learn HTML, CSS, and JavaScript from scratch.', 'BEGINNER', 'Technology', NULL, true, false, false, NOW(), NOW()),
  ('e0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004', 'Creative Writing for Primary', 'Develop creative writing skills for young learners.', 'BEGINNER', 'Languages', NULL, true, false, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 9. Sample Course Modules
INSERT INTO course_modules (id, course_id, title, description, sort_order, is_deleted, created_at, updated_at)
VALUES
  ('f0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'Variables and Expressions', 'Understanding variables, constants, and algebraic expressions', 1, false, NOW(), NOW()),
  ('f0000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000001', 'Solving Equations', 'Linear equations and inequality basics', 2, false, NOW(), NOW()),
  ('f0000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000002', 'Mechanics', 'Forces, motion, and energy', 1, false, NOW(), NOW()),
  ('f0000000-0000-0000-0000-000000000004', 'e0000000-0000-0000-0000-000000000002', 'Electricity and Magnetism', 'Circuits, current, and magnetic fields', 2, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 10. Sample Lessons
INSERT INTO course_lessons (id, module_id, title, content_type, content_url, duration_minutes, sort_order, is_deleted, created_at, updated_at)
VALUES
  ('10000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'What are Variables?', 'VIDEO', NULL, 15, 1, false, NOW(), NOW()),
  ('10000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000001', 'Algebraic Expressions', 'DOCUMENT', NULL, 20, 2, false, NOW(), NOW()),
  ('10000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000002', 'One-Step Equations', 'VIDEO', NULL, 25, 1, false, NOW(), NOW()),
  ('10000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000003', 'Introduction to Forces', 'VIDEO', NULL, 30, 1, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 11. Student enrollments
INSERT INTO learner_enrollments (id, user_id, course_id, institution_id, enrolled_at, progress_percentage, is_deleted, created_at, updated_at)
VALUES
  ('20000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000020', 'e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', NOW(), 25.0, false, NOW(), NOW()),
  ('20000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000020', 'e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', NOW(), 10.0, false, NOW(), NOW()),
  ('20000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000021', 'e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', NOW(), 50.0, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 12. Parent profile row (required by parent_student_links FK)
INSERT INTO parents (id, institution_id, user_id, relationship_type, is_deleted, created_at, updated_at)
VALUES ('c0000000-0000-0000-0000-000000000030', 'a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000030', 'FATHER', false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 12b. Parent-Student linkage
INSERT INTO parent_student_links (id, parent_id, student_id, institution_id, relationship_type, is_deleted, created_at, updated_at)
VALUES
  ('30000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000030', 'b0000000-0000-0000-0000-000000000020', 'a0000000-0000-0000-0000-000000000002', 'FATHER', false, NOW(), NOW()),
  ('30000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000030', 'b0000000-0000-0000-0000-000000000021', 'a0000000-0000-0000-0000-000000000002', 'FATHER', false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 13. Institution memberships
INSERT INTO institution_memberships (user_id, institution_id, role, is_active, created_at, updated_at)
VALUES
  ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000002', 'INSTITUTION_ADMIN', true, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000002', 'TEACHER', true, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000002', 'TEACHER', true, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000020', 'a0000000-0000-0000-0000-000000000002', 'STUDENT', true, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000021', 'a0000000-0000-0000-0000-000000000002', 'STUDENT', true, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000022', 'a0000000-0000-0000-0000-000000000002', 'OTHER_LEARNER', true, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000030', 'a0000000-0000-0000-0000-000000000002', 'PARENT', true, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000012', 'a0000000-0000-0000-0000-000000000003', 'INSTRUCTOR', true, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000003', 'INSTITUTION_ADMIN', true, NOW(), NOW()),
  ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'NATIONAL_ADMIN', true, NOW(), NOW());
