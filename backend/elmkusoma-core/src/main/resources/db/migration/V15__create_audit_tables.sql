-- ELMKUSOMA Core - Audit Module
-- V15: Create audit log, activity feed, and security event tables

-- Audit logs (who, what, when, where, before/after)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    user_id UUID,
    user_email VARCHAR(255),
    user_role VARCHAR(50),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    entity_name VARCHAR(255),
    action VARCHAR(20) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_method VARCHAR(10),
    request_url VARCHAR(500),
    response_status INTEGER,
    duration_ms BIGINT,
    session_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_institution ON audit_logs(institution_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_institution_created ON audit_logs(institution_id, created_at);

-- Activity feed (user/institution level)
CREATE TABLE activity_feeds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    user_id UUID NOT NULL,
    actor_name VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    entity_type VARCHAR(100),
    entity_id UUID,
    entity_name VARCHAR(255),
    metadata JSONB,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activity_feeds_institution ON activity_feeds(institution_id);
CREATE INDEX idx_activity_feeds_user ON activity_feeds(user_id);
CREATE INDEX idx_activity_feeds_created_at ON activity_feeds(created_at);
CREATE INDEX idx_activity_feeds_institution_created ON activity_feeds(institution_id, created_at);

-- Security audit events
CREATE TABLE security_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    user_id UUID,
    user_email VARCHAR(255),
    event_type VARCHAR(50) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    location VARCHAR(255),
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    metadata JSONB,
    resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_at TIMESTAMP,
    resolved_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_security_events_institution ON security_events(institution_id);
CREATE INDEX idx_security_events_user ON security_events(user_id);
CREATE INDEX idx_security_events_type ON security_events(event_type);
CREATE INDEX idx_security_events_severity ON security_events(severity);
CREATE INDEX idx_security_events_created_at ON security_events(created_at);
CREATE INDEX idx_security_events_resolved ON security_events(resolved) WHERE resolved = false;
