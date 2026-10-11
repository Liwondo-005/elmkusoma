-- V150: make recording publication idempotent at the database level.
--
-- A finished live class recording is published to two tables: replays (learner-facing) and
-- media_assets (Media Library). Two independent paths can publish the same recording -
-- session completion (manual end or the expiry sweep) and the LiveKit egress_ended webhook -
-- and with object storage the upload routinely finishes after the class has already ended,
-- so the webhook is the normal completion path and the two race on nearly every recording.
-- Repeated webhook deliveries add another overlap on top of that.
--
-- Until now both writes were check-then-insert with no database guard, so two callers that
-- decided at the same moment that "no record exists yet" would both insert one.
--
-- LiveRecordingPublisher serialises publication on the live class row, which is what makes
-- the check safe in practice. These partial unique indexes are the hard guarantee behind it:
-- they hold even if a future call path forgets the lock, and they make a lost-update bug
-- loud instead of silently producing a second Replay visible to learners.
--
-- Partial (WHERE is_deleted = false) because replay and asset creation are both reversible:
-- an explicitly deleted record must stay deleted and must not block publishing a recording
-- again later, which is the existing soft-delete semantic for both tables.

CREATE UNIQUE INDEX IF NOT EXISTS uq_replays_live_session
    ON replays (live_session_id)
    WHERE live_session_id IS NOT NULL AND is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_media_assets_source
    ON media_assets (source_type, source_id)
    WHERE source_type IS NOT NULL AND source_id IS NOT NULL AND is_deleted = false;
