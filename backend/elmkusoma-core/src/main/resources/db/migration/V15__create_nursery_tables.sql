-- V15: Create nursery tables

CREATE TABLE nursery_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    activity_type VARCHAR(50) NOT NULL,
    education_level VARCHAR(50) NOT NULL,
    subject VARCHAR(100),
    difficulty VARCHAR(20) DEFAULT 'EASY',
    duration_minutes INTEGER,
    materials_needed TEXT,
    instructions TEXT,
    image_url VARCHAR(500),
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_nursery_activities_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_nursery_activities_institution ON nursery_activities(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_nursery_activities_type ON nursery_activities(activity_type) WHERE is_deleted = false;

CREATE TABLE nursery_activity_participations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    activity_id UUID NOT NULL,
    student_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    score INTEGER,
    feedback TEXT,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_nap_activity FOREIGN KEY (activity_id) REFERENCES nursery_activities(id),
    CONSTRAINT fk_nap_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_nap_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_nap_activity ON nursery_activity_participations(activity_id) WHERE is_deleted = false;
CREATE INDEX idx_nap_student ON nursery_activity_participations(student_id) WHERE is_deleted = false;

CREATE TABLE nursery_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    milestone_type VARCHAR(50) NOT NULL,
    target_age_months INTEGER,
    achieved_date DATE,
    is_achieved BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_milestones_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_milestones_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_milestones_student ON nursery_milestones(student_id) WHERE is_deleted = false;

CREATE TABLE nursery_report_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    term_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    overall_performance VARCHAR(50),
    social_skills VARCHAR(50),
    emotional_development VARCHAR(50),
    physical_development VARCHAR(50),
    cognitive_development VARCHAR(50),
    creative_expression VARCHAR(50),
    teacher_comments TEXT,
    generated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_nrc_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_nrc_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_nrc_student ON nursery_report_cards(student_id) WHERE is_deleted = false;
