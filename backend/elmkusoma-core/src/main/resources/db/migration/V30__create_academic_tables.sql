-- ELMKUSOMA Core - Academic Module Tables
-- V30: Create academic structure tables

-- Academic years (e.g., 2024, 2025)
CREATE TABLE academic_years (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
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
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_academic_years_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_academic_years_institution ON academic_years(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_academic_years_level ON academic_years(education_level) WHERE is_deleted = false;

-- Terms / Semesters (e.g., Term 1, Semester 2)
CREATE TABLE terms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    term_number INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_terms_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_terms_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_terms_academic_year ON terms(academic_year_id) WHERE is_deleted = false;

-- Grades (structural: Form 1, Year 2, etc.)
CREATE TABLE grades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    education_level VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_grades_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_grades_institution ON grades(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_grades_level ON grades(education_level) WHERE is_deleted = false;

-- Add education_level column to existing subjects table (V5 created the base table)
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS education_level VARCHAR(50);

-- Class groups (specific class instances per term)
CREATE TABLE class_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    grade_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    term_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    section VARCHAR(10),
    capacity INT,
    class_teacher_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_class_groups_grade FOREIGN KEY (grade_id) REFERENCES grades(id),
    CONSTRAINT fk_class_groups_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_class_groups_term FOREIGN KEY (term_id) REFERENCES terms(id),
    CONSTRAINT fk_class_groups_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_class_groups_grade ON class_groups(grade_id) WHERE is_deleted = false;
CREATE INDEX idx_class_groups_term ON class_groups(term_id) WHERE is_deleted = false;
CREATE INDEX idx_class_groups_institution ON class_groups(institution_id) WHERE is_deleted = false;
