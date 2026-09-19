-- V60: Institution Admin ecosystem enhancements
-- Extend institutions table, add institution_audit_log, enable org-scoped security

-- Extend institutions table with new columns
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS banner_url VARCHAR(500);
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS motto VARCHAR(500);
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS founded_year INTEGER;
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS total_capacity INTEGER;
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS enabled_services TEXT;
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS metadata_json TEXT;
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS approved_by VARCHAR(200);

-- Institution admin audit log (scoped to institution)
CREATE TABLE IF NOT EXISTS institution_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    actor_email VARCHAR(200) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ial_institution ON institution_audit_log(institution_id);
CREATE INDEX IF NOT EXISTS idx_ial_actor ON institution_audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_ial_action ON institution_audit_log(action);
CREATE INDEX IF NOT EXISTS idx_ial_created ON institution_audit_log(created_at);

-- Organization activity feed (scoped to institution)
CREATE TABLE IF NOT EXISTS institution_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    actor_id UUID,
    actor_name VARCHAR(200) NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    metadata_json TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ia_institution ON institution_activity(institution_id);
CREATE INDEX IF NOT EXISTS idx_ia_type ON institution_activity(activity_type);
CREATE INDEX IF NOT EXISTS idx_ia_created ON institution_activity(created_at);

-- Organization invitations (pending users)
CREATE TABLE IF NOT EXISTS institution_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    email VARCHAR(200) NOT NULL,
    role VARCHAR(50) NOT NULL,
    invited_by UUID NOT NULL,
    token VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_inv_institution ON institution_invitations(institution_id);
CREATE INDEX IF NOT EXISTS idx_inv_email ON institution_invitations(email);
CREATE INDEX IF NOT EXISTS idx_inv_token ON institution_invitations(token);
