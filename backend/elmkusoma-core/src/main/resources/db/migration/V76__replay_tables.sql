-- V72: replay tables (§45) + per-user replay progress (§45)
-- Matches Replay.java (table name "replays") and new ReplayProgress entity.

CREATE TABLE IF NOT EXISTS replays (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    event_id UUID NOT NULL,
    live_session_id UUID,
    title VARCHAR(300),
    description TEXT,
    recording_url VARCHAR(500),
    duration_seconds INTEGER,
    thumbnail_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    file_size_bytes BIGINT,
    view_count INTEGER NOT NULL DEFAULT 0,
    last_position_seconds INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_replays_event ON replays(event_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_replays_institution ON replays(institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_replays_status ON replays(status) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_replays_live_session ON replays(live_session_id) WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS replay_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    replay_id UUID NOT NULL,
    user_id UUID NOT NULL,
    position_seconds INTEGER NOT NULL DEFAULT 0,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP,
    CONSTRAINT fk_replay_progress_replay FOREIGN KEY (replay_id) REFERENCES replays(id),
    CONSTRAINT uq_replay_progress_user UNIQUE (replay_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_replay_progress_user ON replay_progress(user_id);
