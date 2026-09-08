-- ELMKUSOMA Core - Enrollment Tables
-- V7: Create enrollment and transfer tables

CREATE TABLE enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
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
    institution_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    from_class_group_id UUID NOT NULL,
    to_class_group_id UUID NOT NULL,
    reason VARCHAR(500),
    transferred_at TIMESTAMP NOT NULL DEFAULT NOW(),
    transferred_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_transfer_records_enrollment ON transfer_records(enrollment_id) WHERE is_deleted = false;
CREATE INDEX idx_transfer_records_institution ON transfer_records(institution_id) WHERE is_deleted = false;
