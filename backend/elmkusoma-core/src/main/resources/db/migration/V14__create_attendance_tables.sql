-- V14: Create attendance tables

CREATE TABLE attendance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    record_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    remarks TEXT,
    marked_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_attendance_class_group FOREIGN KEY (class_group_id) REFERENCES classes(id),
    CONSTRAINT fk_attendance_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_attendance_student ON attendance_records(student_id) WHERE is_deleted = false;
CREATE INDEX idx_attendance_class_date ON attendance_records(class_group_id, record_date) WHERE is_deleted = false;

CREATE TABLE attendance_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    term_id UUID NOT NULL,
    total_days INTEGER NOT NULL DEFAULT 0,
    present_days INTEGER NOT NULL DEFAULT 0,
    absent_days INTEGER NOT NULL DEFAULT 0,
    late_days INTEGER NOT NULL DEFAULT 0,
    excused_days INTEGER NOT NULL DEFAULT 0,
    attendance_percentage DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_summary_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_summary_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_summary_student ON attendance_summaries(student_id) WHERE is_deleted = false;

CREATE TABLE bulk_attendance_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    record_date DATE NOT NULL,
    total_marked INTEGER NOT NULL DEFAULT 0,
    marked_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_bulk_session_class FOREIGN KEY (class_group_id) REFERENCES classes(id),
    CONSTRAINT fk_bulk_session_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);
