-- V11: Create learning tables

CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    content_text TEXT,
    video_url VARCHAR(500),
    file_attachments VARCHAR(500),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_lessons_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_lessons_subject ON lessons(subject_id) WHERE is_deleted = false;
CREATE INDEX idx_lessons_class_group ON lessons(class_group_id) WHERE is_deleted = false;

CREATE TABLE lesson_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    student_id UUID NOT NULL,
    completion_percentage INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id),
    CONSTRAINT fk_progress_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_progress_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_progress_lesson ON lesson_progress(lesson_id) WHERE is_deleted = false;
CREATE INDEX idx_progress_student ON lesson_progress(student_id) WHERE is_deleted = false;

CREATE TABLE assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    due_date TIMESTAMP,
    total_marks INTEGER NOT NULL DEFAULT 100,
    attachments VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_assignments_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_assignments_cg ON assignments(class_group_id) WHERE is_deleted = false;

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
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id),
    CONSTRAINT fk_submissions_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_submissions_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_submissions_assignment ON assignment_submissions(assignment_id) WHERE is_deleted = false;

CREATE TABLE resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    subject_id UUID,
    class_group_id UUID,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    is_public BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_resources_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);
