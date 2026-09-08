-- ELMKUSOMA Core - Developer 05 Migrations
-- V10: Create grading tables

-- Grading Scale configuration per institution
CREATE TABLE grading_scales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    scale_type VARCHAR(50) NOT NULL, -- LETTER, NUMERIC, PERCENTAGE
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_grading_scales_institution ON grading_scales(institution_id) WHERE is_deleted = false;

-- Grade Boundaries (A, B, C, D, F or 90-100, 80-89, etc.)
CREATE TABLE grade_boundaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    grading_scale_id UUID NOT NULL,
    grade_label VARCHAR(10) NOT NULL, -- A, B, C, D, F or 90, 80, 70, etc.
    grade_name VARCHAR(100), -- Excellent, Good, Average, etc.
    min_percentage DECIMAL(5,2) NOT NULL,
    max_percentage DECIMAL(5,2) NOT NULL,
    gpa_points DECIMAL(3,2), -- 4.00, 3.00, 2.00, etc.
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_grade_boundaries_scale ON grade_boundaries(grading_scale_id) WHERE is_deleted = false;
CREATE INDEX idx_grade_boundaries_institution ON grade_boundaries(institution_id) WHERE is_deleted = false;

-- Report Cards
CREATE TABLE report_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    term_id UUID NOT NULL,
    grading_scale_id UUID NOT NULL,
    total_marks DECIMAL(7,2),
    average_mark DECIMAL(5,2),
    overall_grade VARCHAR(10),
    gpa DECIMAL(3,2),
    class_rank INT,
    total_students_in_class INT,
    remarks VARCHAR(1000),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED
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

-- Subject Grades (per report card per subject)
CREATE TABLE subject_grades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    report_card_id UUID NOT NULL,
    subject_id UUID NOT NULL,
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

-- Grading Rubrics
CREATE TABLE grading_rubrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    subject_id UUID,
    total_points DECIMAL(5,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_grading_rubrics_institution ON grading_rubrics(institution_id) WHERE is_deleted = false;

-- Rubric Criteria
CREATE TABLE rubric_criteria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    rubric_id UUID NOT NULL,
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