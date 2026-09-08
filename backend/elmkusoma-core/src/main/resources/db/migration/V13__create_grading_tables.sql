-- V13: Create grading tables

CREATE TABLE grading_scales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_grading_scales_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_grading_scales_institution ON grading_scales(institution_id) WHERE is_deleted = false;

CREATE TABLE grade_boundaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    grading_scale_id UUID NOT NULL,
    grade_letter VARCHAR(10) NOT NULL,
    min_percentage DECIMAL(5,2) NOT NULL,
    max_percentage DECIMAL(5,2) NOT NULL,
    description VARCHAR(200),
    gpa_points DECIMAL(3,1),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_boundaries_scale FOREIGN KEY (grading_scale_id) REFERENCES grading_scales(id),
    CONSTRAINT fk_boundaries_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_boundaries_scale ON grade_boundaries(grading_scale_id) WHERE is_deleted = false;

CREATE TABLE report_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    term_id UUID NOT NULL,
    total_marks INTEGER,
    average_marks DECIMAL(5,2),
    overall_grade VARCHAR(10),
    rank_in_class INTEGER,
    remarks TEXT,
    generated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_report_cards_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_report_cards_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_report_cards_student ON report_cards(student_id) WHERE is_deleted = false;

CREATE TABLE subject_grades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    report_card_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    marks_obtained DECIMAL(5,2),
    grade VARCHAR(10),
    comments TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_subject_grades_report FOREIGN KEY (report_card_id) REFERENCES report_cards(id),
    CONSTRAINT fk_subject_grades_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_subject_grades_report ON subject_grades(report_card_id) WHERE is_deleted = false;

CREATE TABLE grading_rubrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    total_criteria INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_rubrics_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE TABLE rubric_criteria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    rubric_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    max_score INTEGER NOT NULL DEFAULT 10,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_rubric_criteria_rubric FOREIGN KEY (rubric_id) REFERENCES grading_rubrics(id),
    CONSTRAINT fk_rubric_criteria_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);
