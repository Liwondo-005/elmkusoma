-- V158: make Replay publication idempotent at the database level.
--
-- A finished live class recording is published to two tables: replays (learner-facing) and
-- media_assets (Media Library). V150 already makes the media_assets side unique, scoped to
-- LIVE_CLASS and deliberately excluding teacher-uploaded assets. The replays side had no
-- equivalent guarantee.
--
-- Two independent paths can publish the Replay: session completion (manual end or the expiry
-- sweep) and the LiveKit egress_ended webhook (LiveRecordingPublisher). They overlap whenever
-- the upload finishes near the moment the class ends, which with object storage is the normal
-- case rather than the exception. LiveRecordingPublisher serialises publication on the live
-- class row, which makes the check safe in practice. This index is the hard guarantee behind
-- it: it holds even if a future call path forgets the lock, and it makes a lost-update bug
-- loud instead of silently giving learners two Replays for one recording.
--
-- Partial (WHERE live_session_id IS NOT NULL) because replay creation is reversible: an
-- explicitly deleted replay must stay deleted and must not block publishing the recording
-- again later, which is the existing soft-delete semantic. Event replays carry a null
-- live_session_id and are untouched.
--
-- Deliberately NOT scoped on status: the same recording is legitimately re-published when a
-- library entry needs repairing, and that must update one row rather than add another.

CREATE UNIQUE INDEX IF NOT EXISTS uq_replays_live_session
    ON replays (live_session_id)
    WHERE live_session_id IS NOT NULL AND is_deleted = false;
