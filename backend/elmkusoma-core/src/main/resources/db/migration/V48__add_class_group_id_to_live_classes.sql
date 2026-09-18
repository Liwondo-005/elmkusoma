-- V48: Add class_group_id to live_classes table
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS class_group_id UUID;

CREATE INDEX IF NOT EXISTS idx_live_classes_class_group ON live_classes (class_group_id);

COMMENT ON COLUMN live_classes.class_group_id IS 'Optional link to a class group for the live session';
