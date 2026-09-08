-- V7: Create subjects table (consolidated from academic + classes modules)

CREATE TABLE subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    education_level VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    category VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_subjects_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_subjects_institution ON subjects(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_subjects_level ON subjects(education_level) WHERE is_deleted = false;
