-- V2: Identity & Institution module tables

-- Make institution_id nullable on users (for self-registered users not yet linked to an institution)
ALTER TABLE users ALTER COLUMN institution_id DROP NOT NULL;

-- Password reset tokens
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token) WHERE used = false;
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);

-- Email verification tokens
CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_verification_tokens_token ON email_verification_tokens(token) WHERE used = false;
CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens(user_id);

-- Institution memberships
CREATE TABLE institution_memberships (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    institution_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_institution_memberships_user ON institution_memberships(user_id) WHERE is_active = true;
CREATE INDEX idx_institution_memberships_institution ON institution_memberships(institution_id) WHERE is_active = true;
CREATE UNIQUE INDEX idx_institution_memberships_unique ON institution_memberships(user_id, institution_id) WHERE is_active = true;
