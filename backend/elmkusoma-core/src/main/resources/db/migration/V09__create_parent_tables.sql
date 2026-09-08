-- V9: Create parent, student link, and notification preference tables

CREATE TABLE parents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    user_id UUID NOT NULL,
    occupation VARCHAR(255),
    relationship_type VARCHAR(50) NOT NULL DEFAULT 'GUARDIAN',
    emergency_contact VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_parents_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_parents_institution FOREIGN KEY (institution_id) REFERENCES institutions(id),
    CONSTRAINT uq_parents_user UNIQUE (user_id, institution_id)
);

CREATE INDEX idx_parents_institution ON parents(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_parents_user ON parents(user_id) WHERE is_deleted = false;

CREATE TABLE parent_student_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    parent_id UUID NOT NULL,
    student_id UUID NOT NULL,
    relationship_type VARCHAR(50) NOT NULL DEFAULT 'GUARDIAN',
    is_primary BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_links_parent FOREIGN KEY (parent_id) REFERENCES parents(id),
    CONSTRAINT fk_links_institution FOREIGN KEY (institution_id) REFERENCES institutions(id),
    CONSTRAINT uq_parent_student UNIQUE (parent_id, student_id)
);

CREATE INDEX idx_parent_student_links_parent ON parent_student_links(parent_id) WHERE is_deleted = false;
CREATE INDEX idx_parent_student_links_student ON parent_student_links(student_id) WHERE is_deleted = false;

CREATE TABLE parent_notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    parent_id UUID NOT NULL,
    attendance_alerts BOOLEAN NOT NULL DEFAULT true,
    grade_alerts BOOLEAN NOT NULL DEFAULT true,
    fee_alerts BOOLEAN NOT NULL DEFAULT true,
    general_announcements BOOLEAN NOT NULL DEFAULT true,
    sms_enabled BOOLEAN NOT NULL DEFAULT false,
    email_enabled BOOLEAN NOT NULL DEFAULT true,
    push_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_notif_prefs_parent FOREIGN KEY (parent_id) REFERENCES parents(id),
    CONSTRAINT fk_notif_prefs_institution FOREIGN KEY (institution_id) REFERENCES institutions(id),
    CONSTRAINT uq_parent_notif_prefs UNIQUE (parent_id)
);
