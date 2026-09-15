-- V46: Create media_assets table for recordings and media library

CREATE TABLE IF NOT EXISTS media_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    media_type VARCHAR(50) NOT NULL,
    file_url VARCHAR(1000),
    thumbnail_url VARCHAR(500),
    duration_seconds BIGINT,
    file_size_bytes BIGINT,
    mime_type VARCHAR(100),
    status VARCHAR(50) DEFAULT 'READY',
    visibility VARCHAR(50) DEFAULT 'INSTITUTION',
    source_type VARCHAR(50),
    source_id UUID,
    teacher_id UUID,
    course_id UUID,
    subject_id UUID,
    tags VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_media_institution ON media_assets(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_media_type ON media_assets(media_type) WHERE is_deleted = false;
CREATE INDEX idx_media_source ON media_assets(source_type, source_id) WHERE is_deleted = false;
CREATE INDEX idx_media_teacher ON media_assets(teacher_id) WHERE is_deleted = false;
CREATE INDEX idx_media_status ON media_assets(status) WHERE is_deleted = false;
