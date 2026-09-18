CREATE TABLE IF NOT EXISTS teacher_qualifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL REFERENCES teachers(id),
    institution_id UUID NOT NULL,
    qualification_name VARCHAR(255) NOT NULL,
    institution_name VARCHAR(255),
    year_obtained INTEGER,
    document_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_teacher_qualifications_teacher ON teacher_qualifications(teacher_id);
