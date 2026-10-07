-- V135: authoritative actual start time for live classes.
--
-- Auto-expiry was computed from scheduledAt + durationMinutes, so a teacher who
-- started late had their session cut short (scheduled 10:00, duration 5, started
-- 10:02 -> ended 10:05, i.e. three minutes instead of five).
--
-- started_at records when the teacher actually started the session and is the
-- expiry baseline. Nullable on purpose: a SCHEDULED class has not started yet,
-- and existing active rows predate this column. Rows that were already active
-- keep a NULL started_at and fall back to the previous scheduledAt baseline so
-- no in-flight session is stranded. created_at/updated_at/scheduled_at are NOT
-- reused for this: they mean "created", "last modified" and "planned start".

ALTER TABLE live_classes
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP NULL;

-- The expiry sweep scans active statuses and checks the per-row expiry, so no
-- index is required; this index only supports operational lookups of a
-- session's actual start.
CREATE INDEX IF NOT EXISTS idx_live_classes_started_at
    ON live_classes (started_at)
    WHERE is_deleted = false;