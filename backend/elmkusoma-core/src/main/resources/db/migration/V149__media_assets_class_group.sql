-- V149: class-level targeting for Media Library recordings.
--
-- A live class recording was persisted as a Replay but never as a MediaAsset, so the
-- Media Library could not show a class's recordings at all, and media_assets had no way
-- to express which class a recording belongs to (it only had course_id/subject_id).
--
-- This adds the single nullable column that makes the target explicit and queryable.
-- It is deliberately nullable so existing media (none today, but the column must not
-- lock the table) and provider uploads without a class keep working untouched.
--
-- No new table: media_assets already links back to its origin via source_type +
-- source_id, which is what makes recording creation idempotent.

ALTER TABLE media_assets
    ADD COLUMN IF NOT EXISTS class_group_id UUID NULL;

-- Class-targeted lookups: "recordings for this class".
CREATE INDEX IF NOT EXISTS idx_media_assets_class_group
    ON media_assets (class_group_id);

-- Integrity: a recording cannot point at a class group that does not exist. ON DELETE
-- SET NULL rather than cascade, because soft-deleting a class must not delete the
-- library entry - it only detaches the class target.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_media_assets_class_group'
    ) THEN
        ALTER TABLE media_assets
            ADD CONSTRAINT fk_media_assets_class_group
            FOREIGN KEY (class_group_id) REFERENCES class_groups(id) ON DELETE SET NULL;
    END IF;
END$$;