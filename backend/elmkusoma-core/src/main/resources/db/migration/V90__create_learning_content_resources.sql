-- V90: Learning Content Resources - Unified Resource System & Video Tutorials
-- Migrates existing resources table to support unified resource system & video tutorials

-- 1. Resource Type Enum Table
DO $$ BEGIN
    CREATE TYPE resource_type AS ENUM (
        'PDF', 'DOCUMENT', 'PRESENTATION', 'SPREADSHEET', 'IMAGE', 'VIDEO', 
        'AUDIO', 'EXTERNAL_LINK', 'LIVE_RECORDING', 'ARCHIVE', 'OTHER'
    );
EXCEPTION WHEN duplicate_object THEN null; END $$;

-- 2. Resource Visibility Enum
DO $$ BEGIN
    CREATE TYPE resource_visibility AS ENUM (
        'DRAFT', 'PRIVATE', 'CLASS_ONLY', 'COURSE_ONLY', 'SCHOOL', 'INSTITUTION', 'PUBLIC'
    );
EXCEPTION WHEN duplicate_object THEN null; END $$;

-- 3. Video Tutorial Status Enum
DO $$ BEGIN
    CREATE TYPE video_tutorial_status AS ENUM (
        'DRAFT', 'PROCESSING', 'READY', 'FAILED', 'ARCHIVED'
    );
EXCEPTION WHEN duplicate_object THEN null; END $$;

-- 3. Migrate existing resources table to new schema
-- Add new columns to existing resources table
ALTER TABLE resources ADD COLUMN IF NOT EXISTS lesson_id UUID;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS module_id UUID;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS course_id UUID;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS uploaded_by UUID;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100);
ALTER TABLE resources ADD COLUMN IF NOT EXISTS file_size BIGINT;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS storage_url VARCHAR(1000);
ALTER TABLE resources ADD COLUMN IF NOT EXISTS storage_object_key VARCHAR(500);
ALTER TABLE resources ADD COLUMN IF NOT EXISTS storage_bucket VARCHAR(100);
ALTER TABLE resources ADD COLUMN IF NOT EXISTS external_url VARCHAR(1000);
ALTER TABLE resources ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(500);
ALTER TABLE resources ADD COLUMN IF NOT EXISTS duration_seconds INTEGER;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS page_count INTEGER;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS width INTEGER;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS height INTEGER;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS visibility resource_visibility;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS sort_order INTEGER;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS is_downloadable BOOLEAN;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS is_previewable BOOLEAN;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS processing_status VARCHAR(30);
ALTER TABLE resources ADD COLUMN IF NOT EXISTS processing_error TEXT;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS metadata JSONB;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS tags VARCHAR(500);
ALTER TABLE resources ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

-- Migrate existing columns to new schema
UPDATE resources SET 
    lesson_id = class_group_id,
    course_id = subject_id,
    uploaded_by = NULL,  -- Will be set when resources are created/updated
    description = COALESCE(description, ''),
    mime_type = file_type,
    storage_url = file_url,
    visibility = CASE WHEN is_public THEN 'PUBLIC' ELSE 'DRAFT' END::resource_visibility,
    resource_type = file_type::resource_type,
    is_downloadable = true,
    is_previewable = false,
    processing_status = 'READY',
    tags = '',
    metadata = '{}',
    updated_by = NULL
WHERE lesson_id IS NULL;

-- Set defaults for new columns
UPDATE resources SET visibility = 'DRAFT' WHERE visibility IS NULL;
UPDATE resources SET sort_order = 0 WHERE sort_order IS NULL;
UPDATE resources SET is_downloadable = true WHERE is_downloadable IS NULL;
UPDATE resources SET is_previewable = false WHERE is_previewable IS NULL;
UPDATE resources SET processing_status = 'READY' WHERE processing_status IS NULL;
UPDATE resources SET is_deleted = false WHERE is_deleted IS NULL;

-- Add constraints
ALTER TABLE resources DROP CONSTRAINT IF EXISTS chk_resource_external;
ALTER TABLE resources ADD CONSTRAINT chk_resource_external CHECK (
    resource_type <> 'EXTERNAL_LINK' OR external_url IS NOT NULL
);

ALTER TABLE resources DROP CONSTRAINT IF EXISTS chk_resource_file;
ALTER TABLE resources ADD CONSTRAINT chk_resource_file CHECK (
    resource_type = 'EXTERNAL_LINK' OR storage_url IS NOT NULL
);

-- Add foreign keys
DO $$ BEGIN
    ALTER TABLE resources ADD CONSTRAINT fk_resources_lesson 
        FOREIGN KEY (lesson_id) REFERENCES course_lessons(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    ALTER TABLE resources ADD CONSTRAINT fk_resources_module 
        FOREIGN KEY (module_id) REFERENCES course_modules(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    ALTER TABLE resources ADD CONSTRAINT fk_resources_course 
        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN null; END $$;

-- Add constraints
ALTER TABLE resources DROP CONSTRAINT IF EXISTS chk_resource_external;
ALTER TABLE resources ADD CONSTRAINT chk_resource_external CHECK (
    resource_type <> 'EXTERNAL_LINK' OR external_url IS NOT NULL
);

ALTER TABLE resources DROP CONSTRAINT IF EXISTS chk_resource_file;
ALTER TABLE resources ADD CONSTRAINT chk_resource_file CHECK (
    resource_type = 'EXTERNAL_LINK' OR storage_url IS NOT NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_resources_institution ON resources(institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_resources_lesson ON resources(lesson_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_resources_module ON resources(module_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_resources_course ON resources(course_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_resources_type ON resources(resource_type) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_resources_visibility ON resources(visibility) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_resources_uploaded_by ON resources(uploaded_by) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_resources_type_visibility ON resources(resource_type, visibility) WHERE is_deleted = false;

-- 4. Video Tutorial Entity (distinct from Live recordings)
DO $$ BEGIN
    CREATE TYPE video_tutorial_status AS ENUM (
        'DRAFT', 'PROCESSING', 'READY', 'FAILED', 'ARCHIVED'
    );
EXCEPTION WHEN duplicate_object THEN null; END $$;

CREATE TABLE IF NOT EXISTS video_tutorials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    lesson_id UUID REFERENCES course_lessons(id) ON DELETE SET NULL,
    module_id UUID REFERENCES course_modules(id) ON DELETE SET NULL,
    course_id UUID REFERENCES courses(id) ON DELETE SET NULL,
    created_by UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    duration_seconds INTEGER,
    recording_url VARCHAR(1000),
    recording_object_key VARCHAR(500),
    recording_bucket VARCHAR(100),
    thumbnail_url VARCHAR(500),
    thumbnail_object_key VARCHAR(500),
    caption_url VARCHAR(1000),
    caption_object_key VARCHAR(500),
    status video_tutorial_status NOT NULL DEFAULT 'DRAFT',
    processing_error TEXT,
    processing_started_at TIMESTAMP,
    processing_completed_at TIMESTAMP,
    visibility resource_visibility NOT NULL DEFAULT 'DRAFT',
    sort_order INTEGER DEFAULT 0,
    is_downloadable BOOLEAN DEFAULT true,
    is_previewable BOOLEAN DEFAULT true,
    tags VARCHAR(500),
    metadata JSONB,
    updated_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_video_tutorials_institution ON video_tutorials(institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_video_tutorials_lesson ON video_tutorials(lesson_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_video_tutorials_module ON video_tutorials(module_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_video_tutorials_course ON video_tutorials(course_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_video_tutorials_status ON video_tutorials(status) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_video_tutorials_visibility ON video_tutorials(visibility) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_video_tutorials_created_by ON video_tutorials(created_by) WHERE is_deleted = false;

-- 6. Video Tutorial Progress Tracking (extends existing replay progress pattern)
CREATE TABLE IF NOT EXISTS video_tutorial_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_tutorial_id UUID NOT NULL REFERENCES video_tutorials(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    position_seconds INTEGER DEFAULT 0,
    completed BOOLEAN DEFAULT false,
    completion_percentage DECIMAL(5,2) DEFAULT 0.00,
    last_watched_at TIMESTAMP,
    completed_at TIMESTAMP,
    watch_count INTEGER DEFAULT 0,
    total_watch_time_seconds INTEGER DEFAULT 0,
    last_position_seconds INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uk_video_progress_student UNIQUE (video_tutorial_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_video_progress_student ON video_tutorial_progress(student_id);
CREATE INDEX IF NOT EXISTS idx_video_progress_tutorial ON video_tutorial_progress(video_tutorial_id);

-- 7. Resource Library - Saved/Bookmarked Resources for Students
CREATE TABLE IF NOT EXISTS student_saved_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    saved_at TIMESTAMP NOT NULL DEFAULT NOW(),
    notes TEXT,
    CONSTRAINT uk_student_resource UNIQUE (student_id, resource_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_resources_student ON student_saved_resources(student_id);
CREATE INDEX IF NOT EXISTS idx_saved_resources_resource ON student_saved_resources(resource_id);

-- 8. Resource Tags for categorization
CREATE TABLE IF NOT EXISTS resource_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    color VARCHAR(20),
    is_system BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS resource_taggings (
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES resource_tags(id) ON DELETE CASCADE,
    tagged_by UUID,
    tagged_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (resource_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_resource_taggings_resource ON resource_taggings(resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_taggings_tag ON resource_taggings(tag_id);

-- 9. Resource Comments/Annotations for collaborative learning
CREATE TABLE IF NOT EXISTS resource_annotations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    content TEXT NOT NULL,
    position_data JSONB, -- For positional annotations (page, timestamp, x,y coordinates)
    is_private BOOLEAN DEFAULT true,
    parent_annotation_id UUID REFERENCES resource_annotations(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_annotations_resource ON resource_annotations(resource_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_annotations_student ON resource_annotations(student_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_annotations_parent ON resource_annotations(parent_annotation_id) WHERE is_deleted = false;

-- 10. Resource Usage Analytics
CREATE TABLE IF NOT EXISTS resource_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    view_count INTEGER DEFAULT 0,
    download_count INTEGER DEFAULT 0,
    unique_viewers INTEGER DEFAULT 0,
    unique_downloaders INTEGER DEFAULT 0,
    total_watch_time_seconds BIGINT DEFAULT 0,
    avg_watch_time_seconds DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uk_resource_analytics_daily UNIQUE (resource_id, date)
);

CREATE INDEX IF NOT EXISTS idx_analytics_resource ON resource_analytics(resource_id);
CREATE INDEX IF NOT EXISTS idx_analytics_date ON resource_analytics(date);