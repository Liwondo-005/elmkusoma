-- V1: Create institution and user tables
-- These are the foundation tables that all modules depend on

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Institutions table (tenant root)
CREATE TABLE institutions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    official_name   VARCHAR(500),
    description     TEXT,
    logo_url        VARCHAR(500),
    cover_url       VARCHAR(500),
    address         VARCHAR(500) NOT NULL,
    postal_code     VARCHAR(20),
    city            VARCHAR(100),
    region          VARCHAR(100),
    country         VARCHAR(100) NOT NULL DEFAULT 'Tanzania',
    phone_number    VARCHAR(50),
    email_address   VARCHAR(255),
    website_url     VARCHAR(500),
    registration_number VARCHAR(100),
    type            VARCHAR(30) NOT NULL DEFAULT 'PRIMARY',
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING_SETUP',
    institution_id  UUID,
    created_by      UUID,
    updated_by      UUID,
    deleted_at      TIMESTAMP,
    deleted_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_institutions_code ON institutions(code);
CREATE INDEX idx_institutions_name ON institutions(name);
CREATE INDEX idx_institutions_status ON institutions(status);

-- Users table
CREATE TABLE users (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password                VARCHAR(255) NOT NULL,
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100) NOT NULL,
    phone_number            VARCHAR(50),
    avatar_url              VARCHAR(500),
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified          BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at           TIMESTAMP,
    password_reset_token    VARCHAR(255),
    password_reset_expires_at TIMESTAMP,
    institution_id          UUID REFERENCES institutions(id),
    created_by              UUID,
    updated_by              UUID,
    deleted_at              TIMESTAMP,
    deleted_by              UUID,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_institution ON users(institution_id);
CREATE INDEX idx_users_status ON users(status);

-- Institution memberships (links users to institutions with roles)
CREATE TABLE institution_memberships (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    institution_id  UUID NOT NULL REFERENCES institutions(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    role            VARCHAR(50) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    joined_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_membership_institution_user UNIQUE (institution_id, user_id)
);

CREATE INDEX idx_membership_institution ON institution_memberships(institution_id);
CREATE INDEX idx_membership_user ON institution_memberships(user_id);
CREATE INDEX idx_membership_role ON institution_memberships(role);

-- Update users table to reference the default institution creator
-- This is handled at application level, not DB constraint
