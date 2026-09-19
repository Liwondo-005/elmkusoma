CREATE TABLE IF NOT EXISTS learning_modules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    course_id UUID NOT NULL,
    module_title VARCHAR(300) NOT NULL,
    module_code VARCHAR(50),
    description TEXT,
    credit_hours INTEGER,
    instructor_id UUID,
    semester VARCHAR(20),
    academic_year VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    progress_percent INTEGER DEFAULT 0,
    grade VARCHAR(5),
    total_lessons INTEGER,
    completed_lessons INTEGER DEFAULT 0,
    total_assignments INTEGER,
    completed_assignments INTEGER DEFAULT 0,
    total_assessments INTEGER,
    completed_assessments INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID
);

CREATE TABLE IF NOT EXISTS learning_collaborations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    peer_student_id UUID,
    collaboration_type VARCHAR(30) NOT NULL DEFAULT 'STUDY_GROUP',
    title VARCHAR(300) NOT NULL,
    description TEXT,
    course_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID
);

CREATE TABLE IF NOT EXISTS deep_learning_contents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    course_id UUID,
    module_id UUID,
    title VARCHAR(300) NOT NULL,
    content_type VARCHAR(30) NOT NULL DEFAULT 'ARTICLE',
    content_text TEXT,
    file_url VARCHAR(500),
    difficulty_level VARCHAR(20) DEFAULT 'INTERMEDIATE',
    tags VARCHAR(500),
    is_completed BOOLEAN DEFAULT FALSE,
    time_spent_minutes INTEGER DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID
);

CREATE TABLE IF NOT EXISTS workshop_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    workshop_type VARCHAR(30) NOT NULL DEFAULT 'WORKSHOP',
    course_id UUID,
    scheduled_at TIMESTAMP,
    duration_minutes INTEGER,
    location VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    max_participants INTEGER,
    current_participants INTEGER DEFAULT 0,
    materials_url VARCHAR(500),
    instructor_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID
);

CREATE TABLE IF NOT EXISTS professional_development_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    goal_type VARCHAR(30) NOT NULL DEFAULT 'SKILL_DEVELOPMENT',
    target_date DATE,
    completed_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    progress_percent INTEGER DEFAULT 0,
    evidence_url VARCHAR(500),
    notes TEXT,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID
);

CREATE INDEX IF NOT EXISTS idx_lm_student ON learning_modules(student_id);
CREATE INDEX IF NOT EXISTS idx_lm_institution ON learning_modules(institution_id);
CREATE INDEX IF NOT EXISTS idx_lc_student ON learning_collaborations(student_id);
CREATE INDEX IF NOT EXISTS idx_lc_institution ON learning_collaborations(institution_id);
CREATE INDEX IF NOT EXISTS idx_dlc_student ON deep_learning_contents(student_id);
CREATE INDEX IF NOT EXISTS idx_dlc_institution ON deep_learning_contents(institution_id);
CREATE INDEX IF NOT EXISTS idx_ws_student ON workshop_sessions(student_id);
CREATE INDEX IF NOT EXISTS idx_ws_institution ON workshop_sessions(institution_id);
CREATE INDEX IF NOT EXISTS idx_pdg_student ON professional_development_goals(student_id);
CREATE INDEX IF NOT EXISTS idx_pdg_institution ON professional_development_goals(institution_id);
