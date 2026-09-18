CREATE TABLE IF NOT EXISTS enrollment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL,
    student_id UUID NOT NULL,
    from_class_group_id UUID,
    to_class_group_id UUID,
    from_academic_year_id UUID,
    to_academic_year_id UUID,
    change_type VARCHAR(50) NOT NULL,
    reason TEXT,
    changed_by UUID,
    institution_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_enrollment_history_student ON enrollment_history(student_id);
