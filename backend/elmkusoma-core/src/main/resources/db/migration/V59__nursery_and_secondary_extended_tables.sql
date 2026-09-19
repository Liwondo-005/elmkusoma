-- V59: Nursery + Secondary extended tables
-- Nursery: stories, daily_quests, feelings_checkin, missions, tanzania_discovery, parent_learning
-- Secondary: concept_bank, problem_bank, error_bank, study_planner

-- NURSERY STORIES
CREATE TABLE nursery_stories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    content TEXT NOT NULL,
    story_type VARCHAR(50) NOT NULL,
    illustration_url VARCHAR(500),
    audio_url VARCHAR(500),
    duration_minutes INTEGER,
    reading_level VARCHAR(50),
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- NURSERY DAILY QUESTS
CREATE TABLE nursery_daily_quests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    student_id UUID,
    quest_title VARCHAR(300) NOT NULL,
    quest_description TEXT,
    quest_type VARCHAR(50) NOT NULL,
    reward_points INTEGER DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    due_date DATE,
    completed_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- NURSERY FEELINGS CHECK-IN
CREATE TABLE nursery_feelings_checkin (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    feeling VARCHAR(30) NOT NULL,
    emoji VARCHAR(10),
    note TEXT,
    checkin_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- NURSERY MISSIONS
CREATE TABLE nursery_missions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    student_id UUID,
    mission_title VARCHAR(300) NOT NULL,
    mission_description TEXT,
    mission_type VARCHAR(50) NOT NULL,
    reward_points INTEGER DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    due_date DATE,
    completed_date DATE,
    evidence_notes TEXT,
    evidence_image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- NURSERY TANZANIA DISCOVERY
CREATE TABLE nursery_tanzania_discovery (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    class_group_id UUID,
    topic_title VARCHAR(300) NOT NULL,
    topic_description TEXT,
    category VARCHAR(50) NOT NULL,
    region VARCHAR(100),
    fun_facts TEXT,
    image_url VARCHAR(500),
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- NURSERY PARENT LEARNING
CREATE TABLE nursery_parent_learning (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    activity_title VARCHAR(300) NOT NULL,
    activity_description TEXT,
    activity_type VARCHAR(50) NOT NULL,
    parent_name VARCHAR(200),
    completion_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    completed_date DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- SECONDARY CONCEPT BANK
CREATE TABLE secondary_concept_bank (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    concept_name VARCHAR(300) NOT NULL,
    concept_description TEXT NOT NULL,
    examples TEXT,
    related_concepts VARCHAR(500),
    difficulty_level VARCHAR(30) NOT NULL DEFAULT 'BASIC',
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- SECONDARY PROBLEM BANK
CREATE TABLE secondary_problem_bank (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    problem_title VARCHAR(300) NOT NULL,
    problem_description TEXT NOT NULL,
    problem_type VARCHAR(30) NOT NULL,
    options TEXT,
    correct_answer TEXT NOT NULL,
    solution TEXT,
    difficulty_level VARCHAR(30) NOT NULL DEFAULT 'BASIC',
    marks INTEGER DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- SECONDARY ERROR BANK
CREATE TABLE secondary_error_bank (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    error_title VARCHAR(300) NOT NULL,
    error_description TEXT NOT NULL,
    incorrect_example TEXT,
    correct_example TEXT,
    explanation TEXT NOT NULL,
    category VARCHAR(100),
    frequency VARCHAR(30) DEFAULT 'COMMON',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- SECONDARY STUDY PLANNER
CREATE TABLE secondary_study_planner (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    subject_id UUID,
    topic_name VARCHAR(300) NOT NULL,
    planned_date DATE NOT NULL,
    duration_minutes INTEGER DEFAULT 30,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    notes TEXT,
    completed_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
