-- V24: Create event, event_registration, and event_material tables

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    organizer_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    event_type VARCHAR(50) NOT NULL,
    category VARCHAR(100),
    location VARCHAR(500),
    meeting_url VARCHAR(500),
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP,
    duration_minutes INTEGER DEFAULT 60,
    max_participants INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    thumbnail_url VARCHAR(500),
    tags VARCHAR(500),
    is_free BOOLEAN NOT NULL DEFAULT true,
    requires_approval BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_events_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_events_institution ON events(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_events_status ON events(status) WHERE is_deleted = false;
CREATE INDEX idx_events_type ON events(event_type) WHERE is_deleted = false;
CREATE INDEX idx_events_starts_at ON events(starts_at) WHERE is_deleted = false;

CREATE TABLE event_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    registered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    attended BOOLEAN DEFAULT false,
    attended_at TIMESTAMP,
    certificate_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_event_reg_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT uq_event_registration UNIQUE (event_id, user_id)
);

CREATE INDEX idx_event_reg_event ON event_registrations(event_id) WHERE is_deleted = false;
CREATE INDEX idx_event_reg_user ON event_registrations(user_id) WHERE is_deleted = false;

CREATE TABLE event_materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    material_type VARCHAR(50) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT,
    duration_minutes INTEGER,
    sort_order INTEGER DEFAULT 0,
    is_public BOOLEAN NOT NULL DEFAULT true,
    uploaded_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_event_mat_event FOREIGN KEY (event_id) REFERENCES events(id)
);

CREATE INDEX idx_event_materials_event ON event_materials(event_id) WHERE is_deleted = false;
