CREATE TABLE competencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    competency_type VARCHAR(30) NOT NULL,
    programme_id UUID,
    subject_id UUID,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_competencies_institution ON competencies(institution_id);
CREATE INDEX idx_competencies_subject ON competencies(subject_id);
CREATE INDEX idx_competencies_programme ON competencies(programme_id);

CREATE TABLE competency_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    competency_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    evidence TEXT,
    assessed_by UUID,
    assessment_date DATE,
    last_practice_date DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_competency_records_student ON competency_records(student_id);
CREATE INDEX idx_competency_records_competency ON competency_records(competency_id);

ALTER TABLE competency_records ADD CONSTRAINT uq_competency_record_student_competency
    UNIQUE (student_id, competency_id);

CREATE TABLE competency_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    competency_id UUID NOT NULL,
    assessment_id UUID NOT NULL,
    weight INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_competency_assessments_competency ON competency_assessments(competency_id);
CREATE INDEX idx_competency_assessments_assessment ON competency_assessments(assessment_id);

ALTER TABLE competency_assessments ADD CONSTRAINT uq_competency_assessment_competency_assessment
    UNIQUE (competency_id, assessment_id);
