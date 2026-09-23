-- V73: live streaming data-integrity + live-class replay support
-- Recorded for Flyway history. Flyway is skipped at startup (managed externally),
-- so this file is also applied manually via psql. All statements are idempotent.

-- These two tables declare is_deleted NOT NULL with no DB default. Rows are built
-- with Lombok @SuperBuilder (which drops the BaseEntity "= false" initializer), and
-- @DynamicInsert then omits the NULL column, producing NOT NULL violations on
-- WebSocket heartbeat / hand-raise saves.
ALTER TABLE live_class_attendance_detail ALTER COLUMN is_deleted SET DEFAULT false;
ALTER TABLE live_class_hand_raise_queue ALTER COLUMN is_deleted SET DEFAULT false;

-- Replays for live classes have no event row: event_id must be nullable
-- (live_session_id carries the link instead).
ALTER TABLE replays ALTER COLUMN event_id DROP NOT NULL;

-- Ensure the live-session link column exists (entity maps it).
ALTER TABLE replays ADD COLUMN IF NOT EXISTS live_session_id uuid;
