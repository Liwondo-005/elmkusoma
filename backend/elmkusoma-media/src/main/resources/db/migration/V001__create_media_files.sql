CREATE TABLE IF NOT EXISTS media_files (
    id BIGSERIAL PRIMARY KEY,
    institution_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    bucket VARCHAR(255) NOT NULL,
    url VARCHAR(1000),
    thumbnail_url VARCHAR(1000),
    metadata TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_media_files_institution_id ON media_files(institution_id);
CREATE INDEX IF NOT EXISTS idx_media_files_user_id ON media_files(user_id);
CREATE INDEX IF NOT EXISTS idx_media_files_content_type ON media_files(content_type);
CREATE INDEX IF NOT EXISTS idx_media_files_is_deleted ON media_files(is_deleted);
CREATE INDEX IF NOT EXISTS idx_media_files_object_key ON media_files(object_key);
CREATE INDEX IF NOT EXISTS idx_media_files_created_at ON media_files(created_at DESC);
