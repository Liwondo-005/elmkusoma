CREATE TABLE student_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    subject_id UUID,
    title VARCHAR(255) NOT NULL,
    objective VARCHAR(500),
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'IDEATION',
    instructor_id UUID,
    start_date DATE,
    due_date DATE,
    completed_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_student_projects_institution ON student_projects(institution_id);
CREATE INDEX idx_student_projects_student ON student_projects(student_id);
CREATE INDEX idx_student_projects_subject ON student_projects(subject_id);
CREATE INDEX idx_student_projects_instructor ON student_projects(instructor_id);
CREATE INDEX idx_student_projects_status ON student_projects(status);

CREATE TABLE project_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    project_id UUID NOT NULL,
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

CREATE INDEX idx_project_milestones_project ON project_milestones(project_id);

CREATE TABLE project_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    project_id UUID NOT NULL,
    milestone_id UUID,
    submission_type VARCHAR(30) NOT NULL,
    title VARCHAR(255),
    file_url VARCHAR(500),
    description TEXT,
    feedback TEXT,
    grade VARCHAR(50),
    submitted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_project_submissions_project ON project_submissions(project_id);
CREATE INDEX idx_project_submissions_milestone ON project_submissions(milestone_id);

CREATE TABLE fieldwork_placements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    programme_id UUID,
    organisation_name VARCHAR(255) NOT NULL,
    placement_title VARCHAR(255) NOT NULL,
    supervisor_name VARCHAR(255),
    supervisor_email VARCHAR(255),
    supervisor_phone VARCHAR(50),
    institution_supervisor_id UUID,
    start_date DATE,
    end_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNING',
    total_hours_required INTEGER,
    total_hours_completed INTEGER DEFAULT 0,
    objectives TEXT,
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_fieldwork_placements_institution ON fieldwork_placements(institution_id);
CREATE INDEX idx_fieldwork_placements_student ON fieldwork_placements(student_id);
CREATE INDEX idx_fieldwork_placements_programme ON fieldwork_placements(programme_id);
CREATE INDEX idx_fieldwork_placements_supervisor ON fieldwork_placements(institution_supervisor_id);
CREATE INDEX idx_fieldwork_placements_status ON fieldwork_placements(status);

CREATE TABLE logbook_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    placement_id UUID NOT NULL,
    entry_date DATE NOT NULL,
    activities TEXT NOT NULL,
    hours_worked DOUBLE PRECISION,
    skills_used TEXT,
    challenges TEXT,
    learning_outcomes TEXT,
    supervisor_comments TEXT,
    is_approved BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by UUID,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_logbook_entries_placement ON logbook_entries(placement_id);
CREATE INDEX idx_logbook_entries_entry_date ON logbook_entries(entry_date);
CREATE INDEX idx_logbook_entries_approved ON logbook_entries(is_approved);
