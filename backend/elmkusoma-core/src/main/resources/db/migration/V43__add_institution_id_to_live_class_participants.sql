-- V43: Add missing institution_id to live_class_participants (inherited from BaseEntity)
-- Backfill from parent live_classes table, then constrain
-- Uses IF NOT EXISTS to handle cases where Hibernate ddl-auto already added the column

ALTER TABLE live_class_participants ADD COLUMN IF NOT EXISTS institution_id UUID;

-- Backfill from live_classes
UPDATE live_class_participants lcp
SET institution_id = lc.institution_id
FROM live_classes lc
WHERE lcp.live_class_id = lc.id AND lcp.institution_id IS NULL;

-- Now constrain (only if not already NOT NULL)
DO $$
BEGIN
    ALTER TABLE live_class_participants
        ALTER COLUMN institution_id SET NOT NULL;
EXCEPTION
    WHEN not_null_violation OR duplicate_object THEN NULL;
END $$;

-- Add FK if not exists
DO $$
BEGIN
    ALTER TABLE live_class_participants
        ADD CONSTRAINT fk_live_class_participants_institution
        FOREIGN KEY (institution_id) REFERENCES institutions(id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;
