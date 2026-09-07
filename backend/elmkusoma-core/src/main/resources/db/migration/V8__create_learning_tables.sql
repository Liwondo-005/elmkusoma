-- ELMKUSOMA Core - Learning Tables
-- V8: Create lessons, progress, assignments, submissions, resources

CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    content_text TEXT,
    video_url VARCHAR(500),
    file_attachments TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_lessons_institution ON lessons(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_lessons_subject ON lessons(subject_id) WHERE is_deleted = false;
CREATE INDEX idx_lessons_class_group ON lessons(class_group_id) WHERE is_deleted = false;

CREATE TABLE lesson_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    student_id UUID NOT NULL,
    completion_percentage DOUBLE PRECISION NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_lesson_progress_lesson ON lesson_progress(lesson_id) WHERE is_deleted = false;
CREATE INDEX idx_lesson_progress_student ON lesson_progress(student_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX idx_lesson_progress_unique
    ON lesson_progress(lesson_id, student_id)
    WHERE is_deleted = false;

CREATE TABLE assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date TIMESTAMP,
    total_marks INTEGER NOT NULL DEFAULT 100,
    attachments TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_assignments_institution ON assignments(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_assignments_subject ON assignments(subject_id) WHERE is_deleted = false;
CREATE INDEX idx_assignments_class_group ON assignments(class_group_id) WHERE is_deleted = false;

CREATE TABLE assignment_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    assignment_id UUID NOT NULL,
    student_id UUID NOT NULL,
    file_url VARCHAR(500),
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    grade INTEGER,
    feedback TEXT,
    graded_at TIMESTAMP,
    graded_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_submissions_assignment ON assignment_submissions(assignment_id) WHERE is_deleted = false;
CREATE INDEX idx_submissions_student ON assignment_submissions(student_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX idx_submissions_unique
    ON assignment_submissions(assignment_id, student_id)
    WHERE is_deleted = false;

CREATE TABLE resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    subject_id UUID,
    class_group_id UUID,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    file_url VARCHAR(500) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_resources_institution ON resources(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_resources_subject ON resources(subject_id) WHERE is_deleted = false;
