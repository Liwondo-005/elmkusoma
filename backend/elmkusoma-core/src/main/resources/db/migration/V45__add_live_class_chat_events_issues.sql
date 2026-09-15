-- V45: Add live class chat messages, session events, and issues tables

-- Chat messages for live classes
CREATE TABLE IF NOT EXISTS live_class_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    live_class_id UUID NOT NULL,
    user_id UUID NOT NULL,
    user_name VARCHAR(255),
    message VARCHAR(2000) NOT NULL,
    message_type VARCHAR(50) DEFAULT 'CHAT',
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_lc_chat_class ON live_class_chat_messages(live_class_id) WHERE is_deleted = false;
CREATE INDEX idx_lc_chat_user ON live_class_chat_messages(user_id) WHERE is_deleted = false;
CREATE INDEX idx_lc_chat_sent ON live_class_chat_messages(live_class_id, sent_at) WHERE is_deleted = false;

-- Session events for attendance tracking
CREATE TABLE IF NOT EXISTS live_class_session_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    live_class_id UUID NOT NULL,
    user_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_lc_event_class ON live_class_session_events(live_class_id) WHERE is_deleted = false;
CREATE INDEX idx_lc_event_user ON live_class_session_events(user_id) WHERE is_deleted = false;
CREATE INDEX idx_lc_event_type ON live_class_session_events(live_class_id, event_type) WHERE is_deleted = false;

-- Issue reporting for live classes
CREATE TABLE IF NOT EXISTS live_class_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    live_class_id UUID NOT NULL,
    user_id UUID NOT NULL,
    issue_type VARCHAR(100) NOT NULL,
    description TEXT,
    severity VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(20) DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_lc_issue_class ON live_class_issues(live_class_id) WHERE is_deleted = false;
CREATE INDEX idx_lc_issue_status ON live_class_issues(status) WHERE is_deleted = false;
