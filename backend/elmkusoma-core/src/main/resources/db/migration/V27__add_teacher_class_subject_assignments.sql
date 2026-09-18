CREATE TABLE IF NOT EXISTS teacher_class_subject_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL REFERENCES teachers(id),
    class_group_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    academic_year_id UUID,
    institution_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_tcsa_teacher ON teacher_class_subject_assignments(teacher_id);
CREATE INDEX idx_tcsa_class ON teacher_class_subject_assignments(class_group_id);
