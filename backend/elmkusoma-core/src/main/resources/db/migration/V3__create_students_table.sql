-- V3: Add institution description and create students table

ALTER TABLE institutions ADD COLUMN description VARCHAR(1000);

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
