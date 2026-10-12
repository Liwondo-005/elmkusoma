-- Chapter markers on a replay recording (A-XX: chapter/timestamp navigation).
--
-- Why this exists: a live-class recording is a single long file, and a learner looking for
-- the worked example or the exercise has to scrub blindly. A chapter is a titled timestamp
-- the recording's owner records while reviewing it, and the player jumps to it.
--
-- Scope: chapters hang off replays (the learner-facing recording row), not live_classes,
-- because that is what the player actually renders and what replay entitlement already
-- guards. A replay is created by the same finalize path that publishes the recording, so
-- the two stay aligned.
--
-- position_seconds is the seek target and is unique per replay: two chapters at the same
-- second would be indistinguishable when clicked, and the ordering is what the player
-- renders. Partial on is_deleted = false so an explicitly deleted marker does not block
-- re-adding a chapter at that timestamp.

CREATE TABLE IF NOT EXISTS replay_chapters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    replay_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    position_seconds INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_replay_chapters_replay FOREIGN KEY (replay_id) REFERENCES replays(id)
);

CREATE INDEX IF NOT EXISTS idx_replay_chapters_replay
    ON replay_chapters (replay_id) WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_replay_chapters_replay_position
    ON replay_chapters (replay_id, position_seconds) WHERE is_deleted = false;
