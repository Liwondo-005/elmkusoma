CREATE TABLE IF NOT EXISTS class_timetable (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_group_id UUID NOT NULL,
    subject_id UUID,
    teacher_id UUID REFERENCES teachers(id),
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(100),
    institution_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_class_timetable_class ON class_timetable(class_group_id);
