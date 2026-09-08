-- V10: Create enrollment tables

CREATE TABLE enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    enrolled_at TIMESTAMP NOT NULL DEFAULT NOW(),
    withdrawn_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_enrollments_class_group FOREIGN KEY (class_group_id) REFERENCES classes(id),
    CONSTRAINT fk_enrollments_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_enrollments_student ON enrollments(student_id) WHERE is_deleted = false;
CREATE INDEX idx_enrollments_class_group ON enrollments(class_group_id) WHERE is_deleted = false;

CREATE TABLE transfer_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    from_class_group_id UUID NOT NULL,
    to_class_group_id UUID NOT NULL,
    reason VARCHAR(500),
    transferred_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_transfers_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
    CONSTRAINT fk_transfers_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);
