-- V43: Add missing institution_id to live_class_participants (inherited from BaseEntity)
-- Backfill from parent live_classes table, then constrain

ALTER TABLE live_class_participants ADD COLUMN institution_id UUID;

-- Backfill from live_classes
UPDATE live_class_participants lcp
SET institution_id = lc.institution_id
FROM live_classes lc
WHERE lcp.live_class_id = lc.id AND lcp.institution_id IS NULL;

-- Now constrain
ALTER TABLE live_class_participants
    ALTER COLUMN institution_id SET NOT NULL;

ALTER TABLE live_class_participants
    ADD CONSTRAINT fk_live_class_participants_institution
    FOREIGN KEY (institution_id) REFERENCES institutions(id);
