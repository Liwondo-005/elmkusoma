-- ELMKUSOMA Core - Student Module Tables
-- V40: Create student class assignments and parent links (students table created in V3)

-- Student-Class assignments (which student is in which class for a given term)
CREATE TABLE student_class_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    term_id UUID NOT NULL,
    assigned_date TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_sca_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_sca_class_group FOREIGN KEY (class_group_id) REFERENCES class_groups(id),
    CONSTRAINT fk_sca_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_sca_term FOREIGN KEY (term_id) REFERENCES terms(id),
    CONSTRAINT fk_sca_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_sca_student ON student_class_assignments(student_id) WHERE is_deleted = false;
CREATE INDEX idx_sca_class_group ON student_class_assignments(class_group_id) WHERE is_deleted = false;
CREATE INDEX idx_sca_term ON student_class_assignments(term_id) WHERE is_deleted = false;

-- Parent-Student links (placeholder for parent module - Dev 03)
CREATE TABLE parent_student_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    parent_user_id UUID NOT NULL,
    student_id UUID NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_psl_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_psl_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_psl_student ON parent_student_links(student_id) WHERE is_deleted = false;
CREATE INDEX idx_psl_parent ON parent_student_links(parent_user_id) WHERE is_deleted = false;
