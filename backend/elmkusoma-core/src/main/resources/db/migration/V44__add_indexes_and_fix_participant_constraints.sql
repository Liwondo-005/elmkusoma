-- V44: Add teacher_id index for live_classes query performance
CREATE INDEX IF NOT EXISTS idx_live_classes_teacher ON live_classes(teacher_id) WHERE is_deleted = false;
