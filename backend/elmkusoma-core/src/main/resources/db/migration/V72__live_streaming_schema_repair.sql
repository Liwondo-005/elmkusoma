-- V72: Repair schema drift for live streaming.
-- V48-V64 were recorded in flyway_schema_history as "pre-existing migration"
-- but were never actually executed, so V61's changes to
-- live_class_participants were never applied and the replays table
-- (mapped by tz.elmkusoma.event.domain.Replay) was never created.
-- Flyway execution is skipped at application startup (see FlywayConfig),
-- so this file is also applied manually; all statements are idempotent.

ALTER TABLE live_class_participants
  ADD COLUMN IF NOT EXISTS hand_raised_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS hand_raise_order INTEGER;

CREATE TABLE IF NOT EXISTS replays (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  institution_id UUID,
  event_id UUID NOT NULL,
  live_session_id UUID,
  title VARCHAR(300),
  description TEXT,
  recording_url VARCHAR(1000),
  duration_seconds INTEGER,
  thumbnail_url VARCHAR(1000),
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

CREATE INDEX IF NOT EXISTS idx_replays_live_session ON replays (live_session_id);
CREATE INDEX IF NOT EXISTS idx_replays_status ON replays (status) WHERE is_deleted = false;
