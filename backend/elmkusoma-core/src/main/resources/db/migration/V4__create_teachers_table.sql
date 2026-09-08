-- V4: Create teachers table

CREATE TABLE teachers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    user_id UUID NOT NULL REFERENCES users(id),
    employee_number VARCHAR(50),
    department VARCHAR(100),
    specialization VARCHAR(200),
    qualification VARCHAR(200),
    years_of_experience INTEGER,
    hire_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_teachers_institution ON teachers(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_teachers_user ON teachers(user_id) WHERE is_deleted = false;
