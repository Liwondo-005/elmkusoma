-- V63: Platform Admin Governance Tables
-- Adds: Service Catalogue, Platform Incidents, Platform Config, Platform Notifications,
--        Admin Delegation, Provider Memberships, Institution Verification Records

-- 1. Service Catalogue
CREATE TABLE IF NOT EXISTS platform_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    requires_verification BOOLEAN NOT NULL DEFAULT FALSE,
    max_seats INTEGER,
    monthly_price NUMERIC(12,2),
    currency VARCHAR(10) DEFAULT 'TZS',
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_platform_services_category ON platform_services(category);
CREATE INDEX idx_platform_services_active ON platform_services(is_active);

-- 2. Provider Service Entitlements (which services each provider has access to)
CREATE TABLE IF NOT EXISTS provider_service_entitlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    provider_id UUID NOT NULL,
    service_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    seats_used INTEGER DEFAULT 0,
    max_seats INTEGER,
    starts_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_pse_provider ON provider_service_entitlements(provider_id);
CREATE INDEX idx_pse_service ON provider_service_entitlements(service_id);
CREATE INDEX idx_pse_status ON provider_service_entitlements(status);

-- 3. Platform Incidents
CREATE TABLE IF NOT EXISTS platform_incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'DETECTED',
    affected_service VARCHAR(100),
    affected_entity_type VARCHAR(50),
    affected_entity_id UUID,
    detected_at TIMESTAMP NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMP,
    contained_at TIMESTAMP,
    resolved_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    assigned_to VARCHAR(255),
    resolution_notes TEXT,
    root_cause TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_incidents_status ON platform_incidents(status);
CREATE INDEX idx_incidents_severity ON platform_incidents(severity);
CREATE INDEX idx_incidents_category ON platform_incidents(category);
CREATE INDEX idx_incidents_detected ON platform_incidents(detected_at);

-- 4. Platform Configuration
CREATE TABLE IF NOT EXISTS platform_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key VARCHAR(255) NOT NULL UNIQUE,
    config_value TEXT,
    config_type VARCHAR(30) NOT NULL DEFAULT 'STRING',
    description TEXT,
    category VARCHAR(50),
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    last_modified_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_platform_config_key ON platform_config(config_key);
CREATE INDEX idx_platform_config_category ON platform_config(category);

-- 5. Platform Notifications (platform-wide broadcasts)
CREATE TABLE IF NOT EXISTS platform_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    title VARCHAR(500) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    target_audience VARCHAR(50),
    target_role VARCHAR(50),
    target_institution_id UUID,
    sent_by VARCHAR(255),
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    read_count INTEGER DEFAULT 0,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_platform_notifications_type ON platform_notifications(notification_type);
CREATE INDEX idx_platform_notifications_audience ON platform_notifications(target_audience);
CREATE INDEX idx_platform_notifications_sent ON platform_notifications(sent_at);

-- 6. Admin Delegation
CREATE TABLE IF NOT EXISTS admin_delegations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    delegator_id UUID NOT NULL,
    delegate_id UUID NOT NULL,
    permissions JSONB NOT NULL,
    scope VARCHAR(50) NOT NULL DEFAULT 'PLATFORM',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    starts_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revoked_by UUID,
    revocation_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_admin_delegations_delegate ON admin_delegations(delegate_id);
CREATE INDEX idx_admin_delegations_status ON admin_delegations(status);

-- 7. Provider Memberships (links users to providers)
CREATE TABLE IF NOT EXISTS provider_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    user_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'STAFF',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_provider_memberships_user ON provider_memberships(user_id);
CREATE INDEX idx_provider_memberships_provider ON provider_memberships(provider_id);

-- 8. Institution Verification Records
CREATE TABLE IF NOT EXISTS verification_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    verification_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    submitted_by UUID,
    reviewed_by UUID,
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMP,
    expires_at TIMESTAMP,
    notes TEXT,
    documents JSONB,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_verification_records_entity ON verification_records(entity_type, entity_id);
CREATE INDEX idx_verification_records_status ON verification_records(status);

-- 9. Platform Audit Trail (extended for platform-wide operations)
CREATE TABLE IF NOT EXISTS platform_audit_trail (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    actor_id UUID,
    actor_email VARCHAR(255),
    actor_role VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id UUID,
    resource_name VARCHAR(255),
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_method VARCHAR(10),
    request_url TEXT,
    response_status INTEGER,
    duration_ms BIGINT,
    context JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_platform_audit_actor ON platform_audit_trail(actor_id);
CREATE INDEX idx_platform_audit_action ON platform_audit_trail(action);
CREATE INDEX idx_platform_audit_resource ON platform_audit_trail(resource_type, resource_id);
CREATE INDEX idx_platform_audit_created ON platform_audit_trail(created_at);

-- Insert default platform config entries
INSERT INTO platform_config (id, config_key, config_value, config_type, description, category, is_public, created_at, is_deleted)
VALUES
    (gen_random_uuid(), 'platform.name', 'ELMKUSOMA', 'STRING', 'Platform display name', 'GENERAL', true, NOW(), false),
    (gen_random_uuid(), 'platform.registration.enabled', 'true', 'BOOLEAN', 'Allow new user registrations', 'SECURITY', true, NOW(), false),
    (gen_random_uuid(), 'platform.provider.auto_approve', 'false', 'BOOLEAN', 'Auto-approve new providers', 'PROVIDERS', false, NOW(), false),
    (gen_random_uuid(), 'platform.institution.auto_approve', 'false', 'BOOLEAN', 'Auto-approve new institutions', 'INSTITUTIONS', false, NOW(), false),
    (gen_random_uuid(), 'platform.live.max_concurrent_sessions', '50', 'INTEGER', 'Maximum concurrent live sessions', 'LIVE', false, NOW(), false),
    (gen_random_uuid(), 'platform.certificates.auto_issue', 'true', 'BOOLEAN', 'Auto-issue certificates on completion', 'CERTIFICATES', false, NOW(), false),
    (gen_random_uuid(), 'platform.maintenance.enabled', 'false', 'BOOLEAN', 'Maintenance mode', 'GENERAL', true, NOW(), false),
    (gen_random_uuid(), 'platform.maintenance.message', 'System is under maintenance. Please try again later.', 'STRING', 'Maintenance mode message', 'GENERAL', true, NOW(), false)
ON CONFLICT (config_key) DO NOTHING;

-- Insert default platform services
INSERT INTO platform_services (id, name, code, description, category, is_active, created_at, is_deleted)
VALUES
    (gen_random_uuid(), 'Courses', 'COURSES', 'Course creation and management', 'LEARNING', true, NOW(), false),
    (gen_random_uuid(), 'Live Classes', 'LIVE_CLASSES', 'Live streaming and virtual classrooms', 'LIVE', true, NOW(), false),
    (gen_random_uuid(), 'Events', 'EVENTS', 'Events, seminars, and workshops', 'EVENTS', true, NOW(), false),
    (gen_random_uuid(), 'Media', 'MEDIA', 'Video, audio, and media hosting', 'MEDIA', true, NOW(), false),
    (gen_random_uuid(), 'Resources', 'RESOURCES', 'Learning resources and documents', 'RESOURCES', true, NOW(), false),
    (gen_random_uuid(), 'Certificates', 'CERTIFICATES', 'Certificate generation and verification', 'CERTIFICATES', true, NOW(), false),
    (gen_random_uuid(), 'Payments', 'PAYMENTS', 'Payment processing and billing', 'COMMERCE', true, NOW(), false),
    (gen_random_uuid(), 'Analytics', 'ANALYTICS', 'Learning and platform analytics', 'ANALYTICS', true, NOW(), false),
    (gen_random_uuid(), 'Communication', 'COMMUNICATION', 'Messaging and notifications', 'COMMUNICATION', true, NOW(), false),
    (gen_random_uuid(), 'Transport', 'TRANSPORT', 'Student transport management', 'SERVICES', false, NOW(), false)
ON CONFLICT (code) DO NOTHING;
