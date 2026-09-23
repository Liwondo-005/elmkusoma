-- V71: D03 event extended columns
-- Aligns the events / event_registrations / event_materials tables with Event.java,
-- EventRegistration.java and EventMaterial.java mappings (ddl-auto: none/validate in prod).

-- ---------------------------------------------------------------------------
-- events: full table definition (IF NOT EXISTS for DBs where V24 never ran)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
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
    event_status VARCHAR(30),
    event_type_enum VARCHAR(50),
    timezone VARCHAR(50),
    max_capacity INTEGER,
    current_registrations INTEGER DEFAULT 0,
    related_course_id VARCHAR(36),
    related_module_id VARCHAR(36),
    related_lesson_id VARCHAR(36),
    recording_url VARCHAR(500),
    recording_status VARCHAR(20),
    provider_id VARCHAR(36),
    presenter_name VARCHAR(200),
    event_format VARCHAR(30),
    difficulty VARCHAR(30),
    target_audience VARCHAR(100),
    prerequisites TEXT,
    learning_outcomes TEXT,
    agenda TEXT,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    rescheduled_from UUID,
    access_level VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_events_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

-- ---------------------------------------------------------------------------
-- events: add every mapped column that does not exist yet
-- ---------------------------------------------------------------------------
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_status VARCHAR(30);
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_type_enum VARCHAR(50);
ALTER TABLE events ADD COLUMN IF NOT EXISTS timezone VARCHAR(50);
ALTER TABLE events ADD COLUMN IF NOT EXISTS max_capacity INTEGER;
ALTER TABLE events ADD COLUMN IF NOT EXISTS current_registrations INTEGER DEFAULT 0;
ALTER TABLE events ADD COLUMN IF NOT EXISTS related_course_id VARCHAR(36);
ALTER TABLE events ADD COLUMN IF NOT EXISTS related_module_id VARCHAR(36);
ALTER TABLE events ADD COLUMN IF NOT EXISTS related_lesson_id VARCHAR(36);
ALTER TABLE events ADD COLUMN IF NOT EXISTS recording_url VARCHAR(500);
ALTER TABLE events ADD COLUMN IF NOT EXISTS recording_status VARCHAR(20);
ALTER TABLE events ADD COLUMN IF NOT EXISTS provider_id VARCHAR(36);
ALTER TABLE events ADD COLUMN IF NOT EXISTS presenter_name VARCHAR(200);
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_format VARCHAR(30);
ALTER TABLE events ADD COLUMN IF NOT EXISTS difficulty VARCHAR(30);
ALTER TABLE events ADD COLUMN IF NOT EXISTS target_audience VARCHAR(100);
ALTER TABLE events ADD COLUMN IF NOT EXISTS prerequisites TEXT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS learning_outcomes TEXT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS agenda TEXT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;
ALTER TABLE events ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500);
ALTER TABLE events ADD COLUMN IF NOT EXISTS rescheduled_from UUID;
ALTER TABLE events ADD COLUMN IF NOT EXISTS access_level VARCHAR(30);

-- Backfill event_status from legacy string status (COMPLETED maps to ENDED)
UPDATE events
SET event_status = CASE
    WHEN status = 'COMPLETED' THEN 'ENDED'
    WHEN status IN ('DRAFT', 'REVIEW', 'PUBLISHED', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED',
                    'PREPARING', 'STARTING', 'LIVE', 'ENDING', 'ENDED',
                    'RECORDING', 'PROCESSING', 'REPLAY_AVAILABLE',
                    'CANCELLED', 'RESCHEDULED', 'FULL', 'FAILED') THEN status
    ELSE 'DRAFT'
END
WHERE event_status IS NULL;

-- ---------------------------------------------------------------------------
-- event_registrations (full definition for DBs where V24 never ran)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
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

ALTER TABLE event_registrations ADD COLUMN IF NOT EXISTS institution_id UUID;

UPDATE event_registrations er
SET institution_id = e.institution_id
FROM events e
WHERE er.event_id = e.id
  AND er.institution_id IS NULL;

-- ---------------------------------------------------------------------------
-- event_materials (full definition for DBs where V24 never ran)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
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

ALTER TABLE event_materials ADD COLUMN IF NOT EXISTS institution_id UUID;

UPDATE event_materials em
SET institution_id = e.institution_id
FROM events e
WHERE em.event_id = e.id
  AND em.institution_id IS NULL;

-- ---------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_events_institution ON events(institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_events_event_status ON events(event_status) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_events_access_level ON events(access_level) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_events_type ON events(event_type) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_events_starts_at ON events(starts_at) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_event_reg_event ON event_registrations(event_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_event_reg_user ON event_registrations(user_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_event_materials_event ON event_materials(event_id) WHERE is_deleted = false;
