-- V20: Student class assignments

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
