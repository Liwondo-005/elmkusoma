-- One media library asset per live class recording, enforced by the database.
--
-- Why this is needed: the recording hand-off (LiveRecordingMediaPublisher on the webhook path,
-- publishRecordingToMediaLibrary on the synchronous finalize path) already looks the asset up
-- before writing, so it is idempotent in normal operation. That is application-level only:
-- two concurrent deliveries (a manual End racing the expiry sweep, or a retried webhook
-- processed on two application instances) can both observe "no existing asset" and both insert,
-- leaving a learner-facing library with two rows for one recording.
--
-- Scope: LIVE_CLASS assets only. The key is (source_type, source_id) = the origin link the
-- library already exposes via GET /v1/media/recordings/{sourceType}/{sourceId}, which returns a
-- collection today only because duplicates were possible. Teacher-uploaded assets are free to
-- share a source key (it is descriptive metadata there), so they are deliberately excluded.
--
-- Partial predicates, matching the existing idx_media_source index:
--   * is_deleted = false - an explicit library deletion stays deleted and must not block a
--     legitimate re-publish of the same recording later.
--   * source_id IS NOT NULL - teacher-uploaded assets carry no origin.

CREATE UNIQUE INDEX IF NOT EXISTS uq_media_assets_live_class_source
    ON media_assets (source_type, source_id)
    WHERE is_deleted = false
      AND source_type = 'LIVE_CLASS'
      AND source_id IS NOT NULL;
