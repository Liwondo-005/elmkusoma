-- V75: complete the events table to match the Event entity
-- The Event entity (tz.elmkusoma.event.domain.Event) maps 21 extension columns that no
-- migration ever created (Flyway is skipped at startup, ddl-auto: none), so authenticated
-- event queries failed with "column e1_0.<name> does not exist" -> HTTP 500.
-- Applied manually per project convention; the file records the change for history.
-- Idempotent on purpose: ADD COLUMN IF NOT EXISTS everywhere.
-- Legacy rows get NULL for optional extension fields (honest - the feature did not exist
-- when they were created); current_registrations defaults to 0 to match the entity default.
ALTER TABLE events ADD COLUMN IF NOT EXISTS agenda TEXT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500);
ALTER TABLE events ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;
ALTER TABLE events ADD COLUMN IF NOT EXISTS current_registrations INTEGER DEFAULT 0;
ALTER TABLE events ADD COLUMN IF NOT EXISTS difficulty VARCHAR(30);
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_format VARCHAR(30);
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_status VARCHAR(30);
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_type_enum VARCHAR(50);
ALTER TABLE events ADD COLUMN IF NOT EXISTS learning_outcomes TEXT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS max_capacity INTEGER;
ALTER TABLE events ADD COLUMN IF NOT EXISTS prerequisites TEXT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS presenter_name VARCHAR(200);
ALTER TABLE events ADD COLUMN IF NOT EXISTS provider_id VARCHAR(36);
ALTER TABLE events ADD COLUMN IF NOT EXISTS recording_status VARCHAR(20);
ALTER TABLE events ADD COLUMN IF NOT EXISTS recording_url VARCHAR(500);
ALTER TABLE events ADD COLUMN IF NOT EXISTS related_course_id VARCHAR(36);
ALTER TABLE events ADD COLUMN IF NOT EXISTS related_lesson_id VARCHAR(36);
ALTER TABLE events ADD COLUMN IF NOT EXISTS related_module_id VARCHAR(36);
ALTER TABLE events ADD COLUMN IF NOT EXISTS rescheduled_from TIMESTAMP;
ALTER TABLE events ADD COLUMN IF NOT EXISTS target_audience VARCHAR(100);
ALTER TABLE events ADD COLUMN IF NOT EXISTS timezone VARCHAR(50);
