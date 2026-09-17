ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS recording_url VARCHAR(500);
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS recording_duration_seconds INTEGER;
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS max_participants INTEGER;
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS current_participants INTEGER DEFAULT 0;
