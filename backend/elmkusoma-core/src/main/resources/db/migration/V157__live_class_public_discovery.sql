-- Public live-class discovery: publication state, access level, cover image and end time.
--
-- The audit found a complete live-session implementation (LiveKit join, attendance, recordings,
-- replays, an authoritative expiry sweep) but no public discovery surface and no way for a class
-- to become visible to an anonymous visitor. StudentLiveClassController has /live-now and
-- /upcoming, but both are institution-scoped and role-gated.
--
-- FOUR ADDITIVE COLUMNS, ON TWO ORTHOGONAL AXES
--
-- The existing `status` column is the *session* lifecycle: SCHEDULED, IN_PROGRESS, STARTING, LIVE,
-- ENDING, COMPLETED, ENDED, CANCELLED, and the service-degraded states. Nothing here changes it,
-- and no new value is added to it, because inventing a second vocabulary for "is this happening
-- now" would contradict the one the classroom, the expiry sweep and the teacher UI already use.
--
-- Publication is a different question - "may an anonymous visitor see this at all?" - and it
-- answers to a different authority (Platform Admin), on a different clock (editorial review), and
-- survives the session finishing. A class that has ENDED is still publicly visible as Past
-- content; a class that is SCHEDULED may still be invisible. Collapsing these into one status
-- column would make neither representable, so publication_status is deliberately separate.
--
-- DARK BY DEFAULT, TWICE OVER
--
-- Every pre-existing row backfills to publication_status = 'DRAFT' and access_level =
-- 'INSTITUTION'. A public query therefore requires BOTH publication_status = 'PUBLISHED' AND
-- access_level = 'PUBLIC', so publishing alone is not enough to expose a class: someone must also
-- declare it publicly joinable. That is deliberate, because the alternative - approval implies
-- exposure - would turn one Platform Admin click into "every teacher's session is on the
-- marketing site". Existing classes stay invisible until an administrator makes two decisions.

-- Publication state. Editorial, owned by Platform Admin.
--   DRAFT          teacher-created, never submitted, invisible
--   PENDING_APPROVAL submitted by the teacher, awaiting a Platform Admin decision
--   APPROVED       vetted and eligible, but not yet announced
--   PUBLISHED      visible to the public discovery endpoints, subject to access_level
--   REJECTED       refused, with review_note explaining why; the teacher may edit and resubmit
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS publication_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

-- Reuses the vocabulary EventServiceImpl already applies to Event.accessLevel, rather than
-- inventing a parallel one. INSTITUTION is the default and matches how every existing class
-- behaves today.
--   PUBLIC        eligible for the public discovery endpoints and the marketing site
--   AUTHENTICATED any signed-in user may discover it, but joining still needs the join flow
--   INSTITUTION   institution members only (default; preserves current behaviour)
--   PRIVATE/INVITED/RESTRICTED  never discoverable publicly
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS access_level VARCHAR(30) NOT NULL DEFAULT 'INSTITUTION';

-- Representative image for cards. NULL is a normal state, not an error: the client renders a
-- branded placeholder rather than a broken image, and no portrait or photograph is ever
-- fabricated to fill it.
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS cover_image_url VARCHAR(1000);

-- When the session actually finished. Previously past-ness was inferred from scheduled_at, which
-- cannot distinguish a 09:00 class that ran for two hours from one that was cancelled. Written by
-- the completion path so "Past Classes" reflects reality rather than arithmetic.
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS ended_at TIMESTAMP;

ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS submitted_for_review_at TIMESTAMP;
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(255);
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS review_note VARCHAR(500);

-- Serves the Live Now query: published + public + in an active session state. Partial so the index
-- holds only the small set of rows that can ever be live rather than every live_classes row.
CREATE INDEX IF NOT EXISTS idx_live_classes_public_live
    ON live_classes (status, started_at DESC)
    WHERE is_deleted = false
      AND publication_status = 'PUBLISHED'
      AND access_level = 'PUBLIC';

-- Serves Upcoming and Past: both are ordered by time over published, public rows.
CREATE INDEX IF NOT EXISTS idx_live_classes_public_scheduled
    ON live_classes (scheduled_at DESC)
    WHERE is_deleted = false
      AND publication_status = 'PUBLISHED'
      AND access_level = 'PUBLIC';

-- Serves the Platform Admin publication queue, which filters on publication_status alone and so
-- cannot use either partial index above.
CREATE INDEX IF NOT EXISTS idx_live_classes_publication_status
    ON live_classes (publication_status, updated_at DESC)
    WHERE is_deleted = false;

-- No backfill statement is needed: PostgreSQL's ADD COLUMN with a NOT NULL DEFAULT applies that
-- default to existing rows, so every pre-existing class lands on DRAFT + INSTITUTION and stays
-- invisible to the public endpoints.