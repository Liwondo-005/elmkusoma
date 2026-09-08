-- =============================================================================
-- ELMKUSOMA Database Initialization Script
-- Consolidated from Flyway migrations V1-V40
-- Resolves duplicate versions (V2, V3) and duplicate table DDL
-- =============================================================================
-- NOTE: This script is for initial database setup only.
-- Flyway will manage ongoing schema changes after this.
-- Run: psql -U postgres -d elmkusoma -f init.sql
-- =============================================================================

-- Drop all existing objects (clean slate)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO elmkusoma;
GRANT ALL ON SCHEMA public TO postgres;

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- V1: Institutions & Users (+ V3 institution description ALTER)
-- =============================================================================

CREATE TABLE institutions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL,
    address VARCHAR(500),
    city VARCHAR(100),
    region VARCHAR(100),
    country VARCHAR(100) NOT NULL DEFAULT 'Tanzania',
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(255),
    logo_url VARCHAR(500),
    description VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_institutions_code ON institutions(code) WHERE is_deleted = false;
CREATE INDEX idx_institutions_type ON institutions(type) WHERE is_deleted = false;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID REFERENCES institutions(id),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_email_verified BOOLEAN NOT NULL DEFAULT false,
    profile_image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_users_institution ON users(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_users_email ON users(email) WHERE is_deleted = false;
CREATE INDEX idx_users_role ON users(role) WHERE is_deleted = false;

-- =============================================================================
-- V2 (merged): Identity, Institution Memberships, Roles, Permissions
-- =============================================================================

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token) WHERE used = false;
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_verification_tokens_token ON email_verification_tokens(token) WHERE used = false;
CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens(user_id);

CREATE TABLE institution_memberships (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_institution_memberships_user ON institution_memberships(user_id) WHERE is_active = true;
CREATE INDEX idx_institution_memberships_institution ON institution_memberships(institution_id) WHERE is_active = true;
CREATE UNIQUE INDEX idx_institution_memberships_unique ON institution_memberships(user_id, institution_id) WHERE is_active = true;

-- Roles (V2 create_roles)
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    is_system BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(institution_id, code)
);

CREATE INDEX idx_roles_institution ON roles(institution_id) WHERE is_deleted = false;

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    code VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    module VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- =============================================================================
-- V4: Teachers
-- =============================================================================

CREATE TABLE teachers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    user_id UUID NOT NULL REFERENCES users(id),
    employee_number VARCHAR(50),
    department VARCHAR(100),
    specialization VARCHAR(200),
    qualification VARCHAR(200),
    years_of_experience INTEGER,
    hire_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_teachers_institution ON teachers(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_teachers_user ON teachers(user_id) WHERE is_deleted = false;

-- =============================================================================
-- V30: Academic Structure (academic_years, terms, grades, subjects, class_groups)
-- Uses V30 for subjects (superset of V5 columns)
-- =============================================================================

CREATE TABLE academic_years (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    education_level VARCHAR(50) NOT NULL,
    year_label VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_academic_years_institution ON academic_years(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_academic_years_level ON academic_years(education_level) WHERE is_deleted = false;

CREATE TABLE terms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    name VARCHAR(100) NOT NULL,
    term_number INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_terms_academic_year ON terms(academic_year_id) WHERE is_deleted = false;

CREATE TABLE grades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    education_level VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_grades_institution ON grades(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_grades_level ON grades(education_level) WHERE is_deleted = false;

CREATE TABLE subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    education_level VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_subjects_institution ON subjects(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_subjects_level ON subjects(education_level) WHERE is_deleted = false;

CREATE TABLE class_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    grade_id UUID NOT NULL REFERENCES grades(id),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    term_id UUID NOT NULL REFERENCES terms(id),
    name VARCHAR(100) NOT NULL,
    section VARCHAR(10),
    capacity INT,
    class_teacher_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_class_groups_grade ON class_groups(grade_id) WHERE is_deleted = false;
CREATE INDEX idx_class_groups_term ON class_groups(term_id) WHERE is_deleted = false;
CREATE INDEX idx_class_groups_institution ON class_groups(institution_id) WHERE is_deleted = false;

-- =============================================================================
-- V40: Students (superset of V3 columns, with FK constraints)
-- =============================================================================

CREATE TABLE students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    user_id UUID NOT NULL REFERENCES users(id),
    admission_number VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    date_of_birth DATE,
    gender VARCHAR(20),
    address VARCHAR(500),
    city VARCHAR(100),
    region VARCHAR(100),
    national_id VARCHAR(100),
    blood_group VARCHAR(10),
    medical_notes TEXT,
    guardian_name VARCHAR(200),
    guardian_phone VARCHAR(20),
    guardian_relationship VARCHAR(50),
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_students_institution ON students(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_students_user ON students(user_id) WHERE is_deleted = false;
CREATE INDEX idx_students_admission ON students(admission_number) WHERE is_deleted = false;
CREATE INDEX idx_students_status ON students(status) WHERE is_deleted = false;

CREATE TABLE student_class_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    student_id UUID NOT NULL REFERENCES students(id),
    class_group_id UUID NOT NULL REFERENCES class_groups(id),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    term_id UUID NOT NULL REFERENCES terms(id),
    assigned_date TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_sca_student ON student_class_assignments(student_id) WHERE is_deleted = false;
CREATE INDEX idx_sca_class_group ON student_class_assignments(class_group_id) WHERE is_deleted = false;
CREATE INDEX idx_sca_term ON student_class_assignments(term_id) WHERE is_deleted = false;

CREATE TABLE parent_student_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    parent_user_id UUID NOT NULL REFERENCES users(id),
    student_id UUID NOT NULL REFERENCES students(id),
    relationship VARCHAR(50) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_psl_student ON parent_student_links(student_id) WHERE is_deleted = false;
CREATE INDEX idx_psl_parent ON parent_student_links(parent_user_id) WHERE is_deleted = false;

-- =============================================================================
-- V5: Classes, Subjects, Junction Tables (skip subjects - using V30)
-- =============================================================================

CREATE TABLE classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    level VARCHAR(50) NOT NULL,
    section VARCHAR(20),
    capacity INTEGER,
    class_teacher_id UUID REFERENCES teachers(id),
    academic_year VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(institution_id, code, academic_year)
);

CREATE INDEX idx_classes_institution ON classes(institution_id) WHERE is_deleted = false;

CREATE TABLE class_subjects (
    class_id UUID NOT NULL REFERENCES classes(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    teacher_id UUID REFERENCES teachers(id),
    PRIMARY KEY (class_id, subject_id)
);

CREATE TABLE student_class_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id),
    class_id UUID NOT NULL REFERENCES classes(id),
    academic_year VARCHAR(20) NOT NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(student_id, class_id, academic_year)
);

-- =============================================================================
-- V6: Courses and Content
-- =============================================================================

CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    subject_id UUID REFERENCES subjects(id),
    title VARCHAR(300) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    level VARCHAR(50) NOT NULL,
    category VARCHAR(100),
    is_published BOOLEAN NOT NULL DEFAULT false,
    is_featured BOOLEAN NOT NULL DEFAULT false,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_courses_institution ON courses(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_courses_subject ON courses(subject_id) WHERE is_deleted = false;

CREATE TABLE course_modules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id),
    title VARCHAR(300) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE course_lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id UUID NOT NULL REFERENCES course_modules(id),
    title VARCHAR(300) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    content_url VARCHAR(500),
    duration_minutes INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_free BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE live_classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    subject_id UUID REFERENCES subjects(id),
    teacher_id UUID NOT NULL REFERENCES teachers(id),
    title VARCHAR(300) NOT NULL,
    description TEXT,
    scheduled_at TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    meeting_url VARCHAR(500),
    max_participants INTEGER,
    recording_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_live_classes_institution ON live_classes(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_live_classes_status ON live_classes(status) WHERE is_deleted = false;

-- =============================================================================
-- V7: Enrollment Tables
-- =============================================================================

CREATE TABLE enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    student_id UUID NOT NULL REFERENCES students(id),
    class_group_id UUID NOT NULL REFERENCES class_groups(id),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    status VARCHAR(50) NOT NULL DEFAULT 'ENROLLED',
    enrolled_at TIMESTAMP NOT NULL DEFAULT NOW(),
    withdrawn_at TIMESTAMP,
    withdraw_reason VARCHAR(500),
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_enrollments_institution ON enrollments(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_enrollments_student ON enrollments(student_id) WHERE is_deleted = false;
CREATE INDEX idx_enrollments_class_group ON enrollments(class_group_id) WHERE is_deleted = false;
CREATE INDEX idx_enrollments_academic_year ON enrollments(academic_year_id) WHERE is_deleted = false;
CREATE INDEX idx_enrollments_status ON enrollments(status) WHERE is_deleted = false;
CREATE UNIQUE INDEX idx_enrollments_unique_active
    ON enrollments(student_id, class_group_id, academic_year_id)
    WHERE is_deleted = false AND status = 'ENROLLED';

CREATE TABLE transfer_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    enrollment_id UUID NOT NULL REFERENCES enrollments(id),
    from_class_group_id UUID NOT NULL REFERENCES class_groups(id),
    to_class_group_id UUID NOT NULL REFERENCES class_groups(id),
    reason VARCHAR(500),
    transferred_at TIMESTAMP NOT NULL DEFAULT NOW(),
    transferred_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_transfer_records_enrollment ON transfer_records(enrollment_id) WHERE is_deleted = false;
CREATE INDEX idx_transfer_records_institution ON transfer_records(institution_id) WHERE is_deleted = false;

-- =============================================================================
-- V8: Learning Tables
-- =============================================================================

CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    class_group_id UUID NOT NULL REFERENCES class_groups(id),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    content_text TEXT,
    video_url VARCHAR(500),
    file_attachments TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_lessons_institution ON lessons(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_lessons_subject ON lessons(subject_id) WHERE is_deleted = false;
CREATE INDEX idx_lessons_class_group ON lessons(class_group_id) WHERE is_deleted = false;

CREATE TABLE lesson_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    lesson_id UUID NOT NULL REFERENCES lessons(id),
    student_id UUID NOT NULL REFERENCES students(id),
    completion_percentage DOUBLE PRECISION NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_lesson_progress_lesson ON lesson_progress(lesson_id) WHERE is_deleted = false;
CREATE INDEX idx_lesson_progress_student ON lesson_progress(student_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX idx_lesson_progress_unique
    ON lesson_progress(lesson_id, student_id)
    WHERE is_deleted = false;

CREATE TABLE assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    class_group_id UUID NOT NULL REFERENCES class_groups(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date TIMESTAMP,
    total_marks INTEGER NOT NULL DEFAULT 100,
    attachments TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_assignments_institution ON assignments(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_assignments_subject ON assignments(subject_id) WHERE is_deleted = false;
CREATE INDEX idx_assignments_class_group ON assignments(class_group_id) WHERE is_deleted = false;

CREATE TABLE assignment_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    assignment_id UUID NOT NULL REFERENCES assignments(id),
    student_id UUID NOT NULL REFERENCES students(id),
    file_url VARCHAR(500),
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    grade INTEGER,
    feedback TEXT,
    graded_at TIMESTAMP,
    graded_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_submissions_assignment ON assignment_submissions(assignment_id) WHERE is_deleted = false;
CREATE INDEX idx_submissions_student ON assignment_submissions(student_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX idx_submissions_unique
    ON assignment_submissions(assignment_id, student_id)
    WHERE is_deleted = false;

CREATE TABLE resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    subject_id UUID REFERENCES subjects(id),
    class_group_id UUID REFERENCES class_groups(id),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    file_url VARCHAR(500) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_resources_institution ON resources(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_resources_subject ON resources(subject_id) WHERE is_deleted = false;

-- =============================================================================
-- V9: Assessment Tables
-- =============================================================================

CREATE TABLE assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    class_group_id UUID NOT NULL REFERENCES class_groups(id),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    time_limit_minutes INTEGER,
    total_marks INTEGER NOT NULL DEFAULT 100,
    pass_marks INTEGER NOT NULL DEFAULT 50,
    is_published BOOLEAN NOT NULL DEFAULT false,
    starts_at TIMESTAMP,
    ends_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_assessments_institution ON assessments(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_assessments_subject ON assessments(subject_id) WHERE is_deleted = false;
CREATE INDEX idx_assessments_class_group ON assessments(class_group_id) WHERE is_deleted = false;

CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    assessment_id UUID NOT NULL REFERENCES assessments(id),
    question_type VARCHAR(50) NOT NULL,
    question_text TEXT NOT NULL,
    marks INTEGER NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_questions_assessment ON questions(assessment_id) WHERE is_deleted = false;

CREATE TABLE options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    question_id UUID NOT NULL REFERENCES questions(id),
    option_text VARCHAR(500) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_options_question ON options(question_id) WHERE is_deleted = false;

CREATE TABLE attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    assessment_id UUID NOT NULL REFERENCES assessments(id),
    student_id UUID NOT NULL REFERENCES students(id),
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    submitted_at TIMESTAMP,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_attempts_assessment ON attempts(assessment_id) WHERE is_deleted = false;
CREATE INDEX idx_attempts_student ON attempts(student_id) WHERE is_deleted = false;

CREATE TABLE answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    attempt_id UUID NOT NULL REFERENCES attempts(id),
    question_id UUID NOT NULL REFERENCES questions(id),
    selected_option_id UUID,
    text_answer TEXT,
    is_correct BOOLEAN,
    marks_obtained INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_answers_attempt ON answers(attempt_id) WHERE is_deleted = false;
CREATE INDEX idx_answers_question ON answers(question_id) WHERE is_deleted = false;

CREATE TABLE assessment_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    assessment_id UUID NOT NULL REFERENCES assessments(id),
    student_id UUID NOT NULL REFERENCES students(id),
    attempt_id UUID NOT NULL REFERENCES attempts(id),
    total_score INTEGER NOT NULL DEFAULT 0,
    is_passed BOOLEAN NOT NULL DEFAULT false,
    graded_by UUID,
    graded_at TIMESTAMP,
    feedback TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_results_assessment ON assessment_results(assessment_id) WHERE is_deleted = false;
CREATE INDEX idx_results_student ON assessment_results(student_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX idx_results_unique
    ON assessment_results(assessment_id, student_id)
    WHERE is_deleted = false;

-- =============================================================================
-- V10: Grading Tables
-- =============================================================================

CREATE TABLE grading_scales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    scale_type VARCHAR(50) NOT NULL,
    min_value DECIMAL(5,2),
    max_value DECIMAL(5,2),
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_grading_scales_institution ON grading_scales(institution_id) WHERE is_deleted = false;

CREATE TABLE grade_boundaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    grading_scale_id UUID NOT NULL REFERENCES grading_scales(id),
    grade_label VARCHAR(10) NOT NULL,
    grade_name VARCHAR(100),
    min_percentage DECIMAL(5,2) NOT NULL,
    max_percentage DECIMAL(5,2) NOT NULL,
    gpa_points DECIMAL(3,2),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_grade_boundaries_scale ON grade_boundaries(grading_scale_id) WHERE is_deleted = false;
CREATE INDEX idx_grade_boundaries_institution ON grade_boundaries(institution_id) WHERE is_deleted = false;

CREATE TABLE report_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    student_id UUID NOT NULL REFERENCES students(id),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    term_id UUID NOT NULL REFERENCES terms(id),
    grading_scale_id UUID NOT NULL REFERENCES grading_scales(id),
    total_marks DECIMAL(7,2),
    average_mark DECIMAL(5,2),
    overall_grade VARCHAR(10),
    gpa DECIMAL(3,2),
    class_rank INT,
    total_students_in_class INT,
    remarks VARCHAR(1000),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_report_cards_student ON report_cards(student_id) WHERE is_deleted = false;
CREATE INDEX idx_report_cards_term ON report_cards(term_id) WHERE is_deleted = false;
CREATE INDEX idx_report_cards_institution ON report_cards(institution_id) WHERE is_deleted = false;

CREATE TABLE subject_grades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    report_card_id UUID NOT NULL REFERENCES report_cards(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    marks_obtained DECIMAL(5,2),
    grade VARCHAR(10),
    grade_points DECIMAL(3,2),
    teacher_remarks VARCHAR(500),
    assessed_by UUID,
    assessed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_subject_grades_report_card ON subject_grades(report_card_id) WHERE is_deleted = false;
CREATE INDEX idx_subject_grades_subject ON subject_grades(subject_id) WHERE is_deleted = false;

CREATE TABLE grading_rubrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    subject_id UUID REFERENCES subjects(id),
    total_points DECIMAL(5,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_grading_rubrics_institution ON grading_rubrics(institution_id) WHERE is_deleted = false;

CREATE TABLE rubric_criteria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    rubric_id UUID NOT NULL REFERENCES grading_rubrics(id),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    max_points DECIMAL(5,2) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_rubric_criteria_rubric ON rubric_criteria(rubric_id) WHERE is_deleted = false;

-- =============================================================================
-- V11: Attendance Tables
-- =============================================================================

CREATE TABLE attendance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    student_id UUID NOT NULL REFERENCES students(id),
    class_group_id UUID NOT NULL REFERENCES class_groups(id),
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    check_in_time TIMESTAMP,
    check_out_time TIMESTAMP,
    marked_by UUID NOT NULL REFERENCES users(id),
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_attendance_student ON attendance_records(student_id) WHERE is_deleted = false;
CREATE INDEX idx_attendance_class_date ON attendance_records(class_group_id, attendance_date) WHERE is_deleted = false;
CREATE INDEX idx_attendance_institution ON attendance_records(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_attendance_date ON attendance_records(attendance_date) WHERE is_deleted = false;
CREATE UNIQUE INDEX idx_attendance_unique_daily
    ON attendance_records(student_id, class_group_id, attendance_date)
    WHERE is_deleted = false;

CREATE TABLE attendance_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    student_id UUID NOT NULL REFERENCES students(id),
    class_group_id UUID NOT NULL REFERENCES class_groups(id),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    term_id UUID NOT NULL REFERENCES terms(id),
    total_school_days INT NOT NULL DEFAULT 0,
    days_present INT NOT NULL DEFAULT 0,
    days_absent INT NOT NULL DEFAULT 0,
    days_late INT NOT NULL DEFAULT 0,
    days_excused INT NOT NULL DEFAULT 0,
    attendance_percentage DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_attendance_summary_student ON attendance_summaries(student_id) WHERE is_deleted = false;
CREATE INDEX idx_attendance_summary_term ON attendance_summaries(term_id) WHERE is_deleted = false;
CREATE INDEX idx_attendance_summary_institution ON attendance_summaries(institution_id) WHERE is_deleted = false;

CREATE TABLE bulk_attendance_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    class_group_id UUID NOT NULL REFERENCES class_groups(id),
    attendance_date DATE NOT NULL,
    marked_by UUID NOT NULL REFERENCES users(id),
    total_students INT NOT NULL,
    marked_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_bulk_attendance_class_date ON bulk_attendance_sessions(class_group_id, attendance_date) WHERE is_deleted = false;
CREATE INDEX idx_bulk_attendance_institution ON bulk_attendance_sessions(institution_id) WHERE is_deleted = false;

-- =============================================================================
-- V12: Nursery Tables
-- =============================================================================

CREATE TABLE nursery_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    class_group_id UUID NOT NULL REFERENCES class_groups(id),
    activity_name VARCHAR(200) NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    description VARCHAR(2000),
    instructions TEXT,
    duration_minutes INT,
    max_participants INT,
    materials_needed VARCHAR(1000),
    learning_objectives VARCHAR(1000),
    age_group VARCHAR(50),
    activity_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    conducted_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_nursery_activities_class_date ON nursery_activities(class_group_id, activity_date) WHERE is_deleted = false;
CREATE INDEX idx_nursery_activities_institution ON nursery_activities(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_nursery_activities_type ON nursery_activities(activity_type) WHERE is_deleted = false;

CREATE TABLE nursery_activity_participations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    activity_id UUID NOT NULL REFERENCES nursery_activities(id),
    student_id UUID NOT NULL REFERENCES students(id),
    participation_level VARCHAR(20),
    engagement_score INT,
    notes VARCHAR(500),
    participated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_nursery_participations_activity ON nursery_activity_participations(activity_id) WHERE is_deleted = false;
CREATE INDEX idx_nursery_participations_student ON nursery_activity_participations(student_id) WHERE is_deleted = false;

CREATE TABLE nursery_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    student_id UUID NOT NULL REFERENCES students(id),
    category VARCHAR(50) NOT NULL,
    milestone_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    expected_age_months INT,
    achieved_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    observed_by UUID,
    evidence_notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_nursery_milestones_student ON nursery_milestones(student_id) WHERE is_deleted = false;
CREATE INDEX idx_nursery_milestones_category ON nursery_milestones(category) WHERE is_deleted = false;
CREATE INDEX idx_nursery_milestones_institution ON nursery_milestones(institution_id) WHERE is_deleted = false;

CREATE TABLE nursery_report_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    student_id UUID NOT NULL REFERENCES students(id),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    term_id UUID NOT NULL REFERENCES terms(id),
    general_remarks VARCHAR(2000),
    teacher_comments TEXT,
    physical_development VARCHAR(1000),
    cognitive_development VARCHAR(1000),
    social_development VARCHAR(1000),
    emotional_development VARCHAR(1000),
    language_development VARCHAR(1000),
    areas_of_strength VARCHAR(1000),
    areas_for_improvement VARCHAR(1000),
    recommendations_for_parents VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP,
    prepared_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_nursery_report_cards_student ON nursery_report_cards(student_id) WHERE is_deleted = false;
CREATE INDEX idx_nursery_report_cards_term ON nursery_report_cards(term_id) WHERE is_deleted = false;
CREATE INDEX idx_nursery_report_cards_institution ON nursery_report_cards(institution_id) WHERE is_deleted = false;

-- =============================================================================
-- V13: Certificate Tables
-- =============================================================================

CREATE TABLE certificate_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    template_type VARCHAR(50) NOT NULL,
    html_content TEXT,
    css_content TEXT,
    logo_url VARCHAR(500),
    signature_line_1 VARCHAR(255),
    signature_line_2 VARCHAR(255),
    signature_line_3 VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_certificate_templates_institution ON certificate_templates(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_certificate_templates_type ON certificate_templates(template_type) WHERE is_deleted = false;

CREATE TABLE certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    template_id UUID NOT NULL REFERENCES certificate_templates(id),
    student_id UUID NOT NULL REFERENCES students(id),
    issued_by UUID NOT NULL REFERENCES users(id),
    serial_number VARCHAR(50) UNIQUE NOT NULL,
    certificate_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    student_name VARCHAR(255) NOT NULL,
    student_id_number VARCHAR(100),
    course_or_programme VARCHAR(255),
    completion_date DATE NOT NULL,
    issue_date TIMESTAMP NOT NULL DEFAULT NOW(),
    expiry_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    verification_code VARCHAR(100) UNIQUE NOT NULL,
    verification_url VARCHAR(500),
    qr_code_url VARCHAR(500),
    instructor_name VARCHAR(255),
    grade VARCHAR(50),
    skills JSONB,
    revoked_reason VARCHAR(500),
    revoked_at TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_certificates_institution ON certificates(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_certificates_student ON certificates(student_id) WHERE is_deleted = false;
CREATE INDEX idx_certificates_serial ON certificates(serial_number) WHERE is_deleted = false;
CREATE INDEX idx_certificates_verification ON certificates(verification_code) WHERE is_deleted = false;
CREATE INDEX idx_certificates_status ON certificates(status) WHERE is_deleted = false;
CREATE INDEX idx_certificates_issued_by ON certificates(issued_by) WHERE is_deleted = false;
CREATE INDEX idx_certificates_template ON certificates(template_id) WHERE is_deleted = false;

CREATE TABLE certificate_sequences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    certificate_type VARCHAR(50) NOT NULL,
    current_number INTEGER NOT NULL DEFAULT 0,
    prefix VARCHAR(20) NOT NULL DEFAULT 'CERT',
    year INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    UNIQUE(institution_id, certificate_type, year)
);

CREATE TABLE transcripts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    student_id UUID NOT NULL REFERENCES students(id),
    issued_by UUID NOT NULL REFERENCES users(id),
    serial_number VARCHAR(50) UNIQUE NOT NULL,
    academic_year VARCHAR(50),
    term VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_subjects INTEGER,
    average_score DECIMAL(5,2),
    class_rank INTEGER,
    remarks VARCHAR(500),
    generated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    issued_at TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_transcripts_institution ON transcripts(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_transcripts_student ON transcripts(student_id) WHERE is_deleted = false;
CREATE INDEX idx_transcripts_serial ON transcripts(serial_number) WHERE is_deleted = false;
CREATE INDEX idx_transcripts_status ON transcripts(status) WHERE is_deleted = false;

CREATE TABLE transcript_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    transcript_id UUID NOT NULL REFERENCES transcripts(id),
    subject_name VARCHAR(255) NOT NULL,
    subject_code VARCHAR(50),
    score DECIMAL(5,2),
    grade VARCHAR(10),
    remarks VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_transcript_entries_transcript ON transcript_entries(transcript_id) WHERE is_deleted = false;

-- =============================================================================
-- V14: Administration Tables
-- =============================================================================

CREATE TABLE system_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    setting_key VARCHAR(255) NOT NULL,
    setting_value JSONB NOT NULL,
    setting_type VARCHAR(50) NOT NULL DEFAULT 'STRING',
    description VARCHAR(500),
    is_public BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(institution_id, setting_key)
);

CREATE INDEX idx_system_settings_institution ON system_settings(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_system_settings_key ON system_settings(setting_key) WHERE is_deleted = false;

CREATE TABLE custom_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_system_role BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(institution_id, name)
);

CREATE INDEX idx_custom_roles_institution ON custom_roles(institution_id) WHERE is_deleted = false;

CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES custom_roles(id),
    permission VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(role_id, permission)
);

CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);

CREATE TABLE user_role_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES custom_roles(id),
    assigned_by UUID,
    assigned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(user_id, role_id)
);

CREATE INDEX idx_user_role_assignments_institution ON user_role_assignments(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_user_role_assignments_user ON user_role_assignments(user_id) WHERE is_deleted = false;

CREATE TABLE data_import_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    imported_by UUID NOT NULL REFERENCES users(id),
    import_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_rows INTEGER DEFAULT 0,
    processed_rows INTEGER DEFAULT 0,
    successful_rows INTEGER DEFAULT 0,
    failed_rows INTEGER DEFAULT 0,
    error_log JSONB,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_data_import_jobs_institution ON data_import_jobs(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_data_import_jobs_status ON data_import_jobs(status) WHERE is_deleted = false;
CREATE INDEX idx_data_import_jobs_imported_by ON data_import_jobs(imported_by) WHERE is_deleted = false;

CREATE TABLE dashboard_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    snapshot_type VARCHAR(50) NOT NULL,
    snapshot_data JSONB NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(institution_id, snapshot_type)
);

CREATE INDEX idx_dashboard_snapshots_institution ON dashboard_snapshots(institution_id);

-- =============================================================================
-- V15: Audit Tables
-- =============================================================================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    user_id UUID,
    user_email VARCHAR(255),
    user_role VARCHAR(50),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    entity_name VARCHAR(255),
    action VARCHAR(20) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_method VARCHAR(10),
    request_url VARCHAR(500),
    response_status INTEGER,
    duration_ms BIGINT,
    session_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_institution ON audit_logs(institution_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_institution_created ON audit_logs(institution_id, created_at);

CREATE TABLE activity_feeds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    user_id UUID NOT NULL REFERENCES users(id),
    actor_name VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    entity_type VARCHAR(100),
    entity_id UUID,
    entity_name VARCHAR(255),
    metadata JSONB,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activity_feeds_institution ON activity_feeds(institution_id);
CREATE INDEX idx_activity_feeds_user ON activity_feeds(user_id);
CREATE INDEX idx_activity_feeds_created_at ON activity_feeds(created_at);
CREATE INDEX idx_activity_feeds_institution_created ON activity_feeds(institution_id, created_at);

CREATE TABLE security_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID REFERENCES institutions(id),
    user_id UUID,
    user_email VARCHAR(255),
    event_type VARCHAR(50) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    location VARCHAR(255),
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    metadata JSONB,
    resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_at TIMESTAMP,
    resolved_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_security_events_institution ON security_events(institution_id);
CREATE INDEX idx_security_events_user ON security_events(user_id);
CREATE INDEX idx_security_events_type ON security_events(event_type);
CREATE INDEX idx_security_events_severity ON security_events(severity);
CREATE INDEX idx_security_events_created_at ON security_events(created_at);
CREATE INDEX idx_security_events_resolved ON security_events(resolved) WHERE resolved = false;

-- =============================================================================
-- Done: 52 tables created with proper FK constraints and indexes
-- =============================================================================
