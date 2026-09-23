-- V70: BATCH 13 — integration registry, webhook events, platform features,
--      role permissions table, audit retention archive, central policy flags

-- 1. Role permission matrix backing table (entity: RolePermission → role_permissions)
CREATE TABLE IF NOT EXISTS role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL,
    permission VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON role_permissions(role_id);

-- 2. Integration registry (M21) — connection status, no secrets stored
CREATE TABLE IF NOT EXISTS integration_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    integration_key VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    connection_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    last_success_at TIMESTAMP,
    failure_count INTEGER NOT NULL DEFAULT 0,
    webhook_status VARCHAR(20),
    retry_status VARCHAR(20),
    config_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    diagnostics TEXT,
    probe_detail TEXT,
    institution_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO integration_status (id, integration_key, display_name, category, connection_status, config_status, created_at)
VALUES
    (gen_random_uuid(), 'livekit', 'LiveKit', 'LIVE', 'UNKNOWN', 'UNKNOWN', NOW()),
    (gen_random_uuid(), 'payment', 'Payment', 'COMMERCE', 'UNKNOWN', 'UNKNOWN', NOW()),
    (gen_random_uuid(), 'email', 'Email', 'NOTIFICATION', 'UNKNOWN', 'UNKNOWN', NOW()),
    (gen_random_uuid(), 'sms', 'SMS', 'NOTIFICATION', 'UNKNOWN', 'UNKNOWN', NOW()),
    (gen_random_uuid(), 'storage', 'Storage', 'INFRASTRUCTURE', 'UNKNOWN', 'UNKNOWN', NOW()),
    (gen_random_uuid(), 'authentication', 'Authentication', 'IDENTITY', 'UNKNOWN', 'UNKNOWN', NOW()),
    (gen_random_uuid(), 'notification', 'Notification Services', 'NOTIFICATION', 'UNKNOWN', 'UNKNOWN', NOW())
ON CONFLICT (integration_key) DO NOTHING;

-- 3. Webhook event log (M21 §55) — payment + LiveKit observable webhook health
CREATE TABLE IF NOT EXISTS webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(30) NOT NULL,
    event_type VARCHAR(100),
    verification_status VARCHAR(20) NOT NULL,
    processing_result VARCHAR(20) NOT NULL,
    error_details TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    received_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP,
    institution_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_webhook_events_source ON webhook_events(source, received_at DESC);
CREATE INDEX IF NOT EXISTS idx_webhook_events_result ON webhook_events(processing_result);

-- 4. Platform feature lifecycle (M28 §63): PLANNED→DEVELOPMENT→TESTING→ROLLOUT→ACTIVE→DEPRECATED→RETIRED
CREATE TABLE IF NOT EXISTS platform_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_key VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    institution_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO platform_features (id, feature_key, name, status, description, created_at)
VALUES
    (gen_random_uuid(), 'live_classes', 'Live Classes', 'ACTIVE', 'Real-time teaching (LiveKit SFU, webhooks, recordings)', NOW()),
    (gen_random_uuid(), 'payments', 'Payments & Entitlements', 'ACTIVE', 'Central billing, webhook verification, entitlement engine', NOW()),
    (gen_random_uuid(), 'certificates', 'Certificates', 'ACTIVE', 'Completion-issued credentials with revocation + verification', NOW()),
    (gen_random_uuid(), 'events', 'Events / Seminars / Webinars', 'ACTIVE', 'Event lifecycle with access models and replays', NOW()),
    (gen_random_uuid(), 'moderation', 'Content Moderation', 'ACTIVE', 'Content reports and review actions', NOW()),
    (gen_random_uuid(), 'support_tickets', 'Support & Case Management', 'ACTIVE', 'Operational case lifecycle with assignment', NOW()),
    (gen_random_uuid(), 'data_export', 'Data Export', 'ACTIVE', 'Audit-logged CSV exports of platform data', NOW()),
    (gen_random_uuid(), 'sponsored_seats', 'Sponsored Seats', 'ACTIVE', 'Payment Model B/C provider-sponsored access', NOW()),
    (gen_random_uuid(), 'integration_registry', 'Integration Registry', 'ROLLOUT', 'Connection status, webhook health, probe diagnostics', NOW()),
    (gen_random_uuid(), 'backup_status', 'Backup Status Surface', 'ROLLOUT', 'Real backup script status surfaced to Platform Admin', NOW()),
    (gen_random_uuid(), 'policy_engine', 'Policy & Governance Engine', 'ROLLOUT', 'Central platform policy flags with enforcement read points', NOW()),
    (gen_random_uuid(), 'maintenance_mode', 'Maintenance Mode', 'ACTIVE', 'Platform-wide maintenance window enforcement', NOW())
ON CONFLICT (feature_key) DO NOTHING;

-- 5. Audit retention: archive flag (§51 — controlled retention, traceable, never silent erase)
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_audit_logs_archived_at ON audit_logs(archived_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);

-- 6. Central policy flags (M27 §62) — defaults preserve current behaviour (permissive = true)
INSERT INTO platform_config (id, config_key, config_value, config_type, description, category, is_public, created_at, is_deleted)
VALUES
    (gen_random_uuid(), 'policy.registration.enabled', 'true', 'BOOLEAN', 'Who can register: allow public registration', 'POLICY', false, NOW(), false),
    (gen_random_uuid(), 'policy.provider.create.enabled', 'true', 'BOOLEAN', 'Who can create providers/institutions', 'POLICY', false, NOW(), false),
    (gen_random_uuid(), 'policy.content.publish.enabled', 'true', 'BOOLEAN', 'Who can publish content', 'POLICY', false, NOW(), false),
    (gen_random_uuid(), 'policy.live.create.enabled', 'true', 'BOOLEAN', 'Who can create Live sessions', 'POLICY', false, NOW(), false),
    (gen_random_uuid(), 'policy.certificate.issue.enabled', 'true', 'BOOLEAN', 'Who can issue certificates', 'POLICY', false, NOW(), false),
    (gen_random_uuid(), 'policy.charge.enabled', 'true', 'BOOLEAN', 'Who can charge users', 'POLICY', false, NOW(), false),
    (gen_random_uuid(), 'policy.sponsor.enabled', 'true', 'BOOLEAN', 'Who can sponsor users', 'POLICY', false, NOW(), false),
    (gen_random_uuid(), 'policy.invite.enabled', 'true', 'BOOLEAN', 'Who can invite participants', 'POLICY', false, NOW(), false),
    (gen_random_uuid(), 'policy.paid_access.enabled', 'true', 'BOOLEAN', 'Who can access paid services', 'POLICY', false, NOW(), false),
    (gen_random_uuid(), 'policy.verification.required', 'true', 'BOOLEAN', 'Who requires verification', 'POLICY', false, NOW(), false),
    (gen_random_uuid(), 'policy.approval.required', 'true', 'BOOLEAN', 'Who requires approval', 'POLICY', false, NOW(), false),
    (gen_random_uuid(), 'ops.scheduler.heartbeat', NULL, 'TIMESTAMP', 'Last background scheduler heartbeat (real)', 'OPS', false, NOW(), false),
    (gen_random_uuid(), 'ops.backup.status_file', './backups/last_backup_status.json', 'STRING', 'Path to backup status file written by backup-db.sh', 'OPS', false, NOW(), false),
    (gen_random_uuid(), 'ops.backup.dir', './backups', 'STRING', 'Directory holding database backup dumps', 'OPS', false, NOW(), false),
    (gen_random_uuid(), 'data.retention.last_sweep', NULL, 'STRING', 'Result of last data retention sweep (JSON, set by job)', 'DATA', false, NOW(), false)
ON CONFLICT (config_key) DO NOTHING;
