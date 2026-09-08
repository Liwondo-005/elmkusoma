-- V6: Create courses and content tables

CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    subject_id UUID REFERENCES subjects(id),
    title VARCHAR(300) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    level VARCHAR(50) NOT NULL,
    category VARCHAR(100),
    is_published BOOLEAN NOT NULL DEFAULT false,
    is_featured BOOLEAN NOT NULL DEFAULT false,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_courses_institution ON courses(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_courses_subject ON courses(subject_id) WHERE is_deleted = false;

CREATE TABLE course_modules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id),
    title VARCHAR(300) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE course_lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id UUID NOT NULL REFERENCES course_modules(id),
    title VARCHAR(300) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    content_url VARCHAR(500),
    duration_minutes INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_free BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE live_classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    subject_id UUID REFERENCES subjects(id),
    teacher_id UUID NOT NULL REFERENCES teachers(id),
    title VARCHAR(300) NOT NULL,
    description TEXT,
    scheduled_at TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    meeting_url VARCHAR(500),
    max_participants INTEGER,
    recording_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_live_classes_institution ON live_classes(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_live_classes_status ON live_classes(status) WHERE is_deleted = false;
