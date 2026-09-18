CREATE TABLE programmes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    programme_type VARCHAR(30) NOT NULL,
    education_level VARCHAR(30),
    duration_months INTEGER,
    credit_hours INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_programmes_institution ON programmes(institution_id);
CREATE INDEX idx_programmes_type ON programmes(programme_type);
CREATE INDEX idx_programmes_education_level ON programmes(education_level);
CREATE INDEX idx_programmes_active ON programmes(is_active) WHERE is_deleted = FALSE;

CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    programme_ids JSONB,
    head_of_department_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_departments_institution ON departments(institution_id);
CREATE INDEX idx_departments_active ON departments(is_active) WHERE is_deleted = FALSE;

ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS session_type VARCHAR(30) DEFAULT 'LECTURE';
