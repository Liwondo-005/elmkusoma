-- V18: Create audit tables

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_audit_logs_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_audit_logs_institution ON audit_logs(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id) WHERE is_deleted = false;
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id) WHERE is_deleted = false;
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at) WHERE is_deleted = false;

CREATE TABLE activity_feeds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    user_id UUID,
    activity_type VARCHAR(50) NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    entity_type VARCHAR(100),
    entity_id UUID,
    metadata JSONB,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_activity_feed_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_activity_feed_institution ON activity_feeds(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_activity_feed_user ON activity_feeds(user_id) WHERE is_deleted = false;

CREATE TABLE security_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    user_id UUID,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    description TEXT NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata JSONB,
    resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_at TIMESTAMP,
    resolved_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_security_events_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_security_events_institution ON security_events(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_security_events_type ON security_events(event_type) WHERE is_deleted = false;
CREATE INDEX idx_security_events_severity ON security_events(severity) WHERE is_deleted = false;
