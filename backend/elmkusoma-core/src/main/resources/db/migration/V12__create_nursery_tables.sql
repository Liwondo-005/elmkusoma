-- ELMKUSOMA Core - Developer 05 Migrations
-- V12: Create nursery tables

-- Nursery Activities (games, songs, stories, etc.)
CREATE TABLE nursery_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    activity_name VARCHAR(200) NOT NULL,
    activity_type VARCHAR(50) NOT NULL, -- GAME, SONG, STORY, CRAFT, PHYSICAL, EDUCATIONAL
    description VARCHAR(2000),
    instructions TEXT,
    duration_minutes INT,
    max_participants INT,
    materials_needed VARCHAR(1000),
    learning_objectives VARCHAR(1000),
    age_group VARCHAR(50), -- 3-4, 4-5, 5-6
    activity_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED', -- PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
    conducted_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_nursery_activities_class_date ON nursery_activities(class_group_id, activity_date) WHERE is_deleted = false;
CREATE INDEX idx_nursery_activities_institution ON nursery_activities(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_nursery_activities_type ON nursery_activities(activity_type) WHERE is_deleted = false;

-- Nursery Activity Participation (tracking student participation)
CREATE TABLE nursery_activity_participations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    activity_id UUID NOT NULL,
    student_id UUID NOT NULL,
    participation_level VARCHAR(20), -- FULL, PARTIAL, MINIMAL, NONE
    engagement_score INT, -- 1-5 scale
    notes VARCHAR(500),
    participated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_nursery_participations_activity ON nursery_activity_participations(activity_id) WHERE is_deleted = false;
CREATE INDEX idx_nursery_participations_student ON nursery_activity_participations(student_id) WHERE is_deleted = false;

-- Nursery Milestones (physical, cognitive, social development)
CREATE TABLE nursery_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    category VARCHAR(50) NOT NULL, -- PHYSICAL, COGNITIVE, SOCIAL, EMOTIONAL, LANGUAGE, MOTOR
    milestone_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    expected_age_months INT,
    achieved_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACHIEVED, IN_PROGRESS, NOT_OBSERVED
    observed_by UUID,
    evidence_notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_nursery_milestones_student ON nursery_milestones(student_id) WHERE is_deleted = false;
CREATE INDEX idx_nursery_milestones_category ON nursery_milestones(category) WHERE is_deleted = false;
CREATE INDEX idx_nursery_milestones_institution ON nursery_milestones(institution_id) WHERE is_deleted = false;

-- Nursery Report Cards (descriptive, not numeric)
CREATE TABLE nursery_report_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    term_id UUID NOT NULL,
    general_remarks VARCHAR(2000),
    teacher_comments TEXT,
    physical_development VARCHAR(1000),
    cognitive_development VARCHAR(1000),
    social_development VARCHAR(1000),
    emotional_development VARCHAR(1000),
    language_development VARCHAR(1000),
    areas_of_strength VARCHAR(1000),
    areas_for_improvement VARCHAR(1000),
    recommendations_for_parents VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED
    published_at TIMESTAMP,
    prepared_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_nursery_report_cards_student ON nursery_report_cards(student_id) WHERE is_deleted = false;
CREATE INDEX idx_nursery_report_cards_term ON nursery_report_cards(term_id) WHERE is_deleted = false;
CREATE INDEX idx_nursery_report_cards_institution ON nursery_report_cards(institution_id) WHERE is_deleted = false;