CREATE TABLE research_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    research_question VARCHAR(500),
    objectives TEXT,
    supervisor_id UUID,
    status VARCHAR(30) NOT NULL DEFAULT 'IDEA',
    programme_id UUID,
    subject_id UUID,
    methodology TEXT,
    start_date DATE,
    due_date DATE,
    completed_date DATE,
    abstract_text TEXT,
    keywords VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_research_projects_institution ON research_projects(institution_id);
CREATE INDEX idx_research_projects_student ON research_projects(student_id);
CREATE INDEX idx_research_projects_status ON research_projects(status);
CREATE INDEX idx_research_projects_supervisor ON research_projects(supervisor_id);
CREATE INDEX idx_research_projects_programme ON research_projects(programme_id);
CREATE INDEX idx_research_projects_subject ON research_projects(subject_id);

CREATE TABLE research_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    research_project_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_date TIMESTAMP,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_research_milestones_project ON research_milestones(research_project_id);
CREATE INDEX idx_research_milestones_due_date ON research_milestones(due_date);

CREATE TABLE research_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    research_project_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    resource_type VARCHAR(20) NOT NULL,
    file_url VARCHAR(500),
    citation TEXT,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_research_resources_project ON research_resources(research_project_id);
CREATE INDEX idx_research_resources_type ON research_resources(resource_type);

CREATE TABLE theses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    research_project_id UUID,
    supervisor_id UUID,
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    programme_id UUID,
    submission_date DATE,
    defense_date DATE,
    final_grade VARCHAR(50),
    abstract_text TEXT,
    word_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_theses_institution ON theses(institution_id);
CREATE INDEX idx_theses_student ON theses(student_id);
CREATE INDEX idx_theses_status ON theses(status);
CREATE INDEX idx_theses_supervisor ON theses(supervisor_id);
CREATE INDEX idx_theses_programme ON theses(programme_id);
CREATE INDEX idx_theses_research_project ON theses(research_project_id);

CREATE TABLE study_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    task_type VARCHAR(20) NOT NULL DEFAULT 'STUDY',
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    subject_id UUID,
    scheduled_date DATE,
    scheduled_time TIME,
    duration_minutes INTEGER,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_date TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_study_tasks_institution ON study_tasks(institution_id);
CREATE INDEX idx_study_tasks_student ON study_tasks(student_id);
CREATE INDEX idx_study_tasks_scheduled_date ON study_tasks(scheduled_date);
CREATE INDEX idx_study_tasks_task_type ON study_tasks(task_type);
CREATE INDEX idx_study_tasks_priority ON study_tasks(priority);
CREATE INDEX idx_study_tasks_subject ON study_tasks(subject_id);
CREATE INDEX idx_study_tasks_student_date ON study_tasks(student_id, scheduled_date);
