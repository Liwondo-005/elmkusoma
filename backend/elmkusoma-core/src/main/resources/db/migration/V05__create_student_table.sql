-- V5: Create students table (consolidated)

CREATE TABLE students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    user_id UUID NOT NULL,
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
    guardian_email VARCHAR(255),
    guardian_relationship VARCHAR(50),
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_students_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_students_institution ON students(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_students_user ON students(user_id) WHERE is_deleted = false;
CREATE INDEX idx_students_admission ON students(admission_number) WHERE is_deleted = false;
CREATE INDEX idx_students_status ON students(status) WHERE is_deleted = false;
