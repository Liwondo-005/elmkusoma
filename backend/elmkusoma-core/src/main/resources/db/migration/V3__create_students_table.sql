-- V3: Create students table

CREATE TABLE students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    user_id UUID NOT NULL REFERENCES users(id),
    admission_number VARCHAR(50),
    date_of_birth DATE,
    gender VARCHAR(20),
    guardian_name VARCHAR(200),
    guardian_phone VARCHAR(20),
    guardian_email VARCHAR(255),
    address VARCHAR(500),
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    current_class VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_students_institution ON students(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_students_user ON students(user_id) WHERE is_deleted = false;
CREATE INDEX idx_students_admission ON students(admission_number) WHERE is_deleted = false;
