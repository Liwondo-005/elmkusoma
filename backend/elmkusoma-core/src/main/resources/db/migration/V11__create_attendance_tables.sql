-- ELMKUSOMA Core - Developer 05 Migrations
-- V11: Create attendance tables

-- Attendance Records
CREATE TABLE attendance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL, -- PRESENT, ABSENT, LATE, EXCUSED
    check_in_time TIMESTAMP,
    check_out_time TIMESTAMP,
    marked_by UUID NOT NULL, -- Teacher who marked attendance
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_attendance_student ON attendance_records(student_id) WHERE is_deleted = false;
CREATE INDEX idx_attendance_class_date ON attendance_records(class_group_id, attendance_date) WHERE is_deleted = false;
CREATE INDEX idx_attendance_institution ON attendance_records(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_attendance_date ON attendance_records(attendance_date) WHERE is_deleted = false;

-- Unique constraint: one attendance record per student per day per class
CREATE UNIQUE INDEX idx_attendance_unique_daily 
ON attendance_records(student_id, class_group_id, attendance_date) 
WHERE is_deleted = false;

-- Attendance Summary (aggregated per term)
CREATE TABLE attendance_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    term_id UUID NOT NULL,
    total_school_days INT NOT NULL DEFAULT 0,
    days_present INT NOT NULL DEFAULT 0,
    days_absent INT NOT NULL DEFAULT 0,
    days_late INT NOT NULL DEFAULT 0,
    days_excused INT NOT NULL DEFAULT 0,
    attendance_percentage DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_attendance_summary_student ON attendance_summaries(student_id) WHERE is_deleted = false;
CREATE INDEX idx_attendance_summary_term ON attendance_summaries(term_id) WHERE is_deleted = false;
CREATE INDEX idx_attendance_summary_institution ON attendance_summaries(institution_id) WHERE is_deleted = false;

-- Bulk Attendance Sessions (for tracking bulk marking operations)
CREATE TABLE bulk_attendance_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    attendance_date DATE NOT NULL,
    marked_by UUID NOT NULL,
    total_students INT NOT NULL,
    marked_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED, CANCELLED
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_bulk_attendance_class_date ON bulk_attendance_sessions(class_group_id, attendance_date) WHERE is_deleted = false;
CREATE INDEX idx_bulk_attendance_institution ON bulk_attendance_sessions(institution_id) WHERE is_deleted = false;