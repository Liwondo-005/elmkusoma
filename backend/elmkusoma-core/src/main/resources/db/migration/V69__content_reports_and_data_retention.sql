-- V69: Content moderation reports + data governance retention config

CREATE TABLE IF NOT EXISTS content_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    entity_title VARCHAR(500),
    reporter_id UUID,
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolution_notes TEXT,
    resolved_by UUID,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_content_reports_status ON content_reports(status);
CREATE INDEX IF NOT EXISTS idx_content_reports_entity ON content_reports(entity_type, entity_id);

-- Data governance retention configuration (editable via /v1/platform-admin/config)
INSERT INTO platform_config (id, config_key, config_value, config_type, description, category, is_public, is_sensitive, created_at, is_deleted)
VALUES
    (gen_random_uuid(), 'data.retention.days.audit', '3650', 'INTEGER', 'Audit log retention in days (floor)', 'DATA', false, false, NOW(), false),
    (gen_random_uuid(), 'data.retention.days.media', '1825', 'INTEGER', 'Media retention in days (floor)', 'DATA', false, false, NOW(), false),
    (gen_random_uuid(), 'data.retention.days.security_events', '730', 'INTEGER', 'Security event retention in days', 'DATA', false, false, NOW(), false),
    (gen_random_uuid(), 'data.export.enabled', 'true', 'BOOLEAN', 'Allow platform data exports', 'DATA', false, false, NOW(), false)
ON CONFLICT (config_key) DO NOTHING;
