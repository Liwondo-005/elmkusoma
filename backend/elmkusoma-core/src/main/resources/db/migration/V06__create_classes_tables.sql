-- V6: Create classes and class-subject enrollment tables

CREATE TABLE classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    level VARCHAR(50) NOT NULL,
    section VARCHAR(20),
    capacity INTEGER,
    class_teacher_id UUID REFERENCES teachers(id),
    academic_year VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(institution_id, code, academic_year)
);

CREATE INDEX idx_classes_institution ON classes(institution_id) WHERE is_deleted = false;

CREATE TABLE class_subjects (
    class_id UUID NOT NULL REFERENCES classes(id),
    subject_id UUID NOT NULL,
    teacher_id UUID REFERENCES teachers(id),
    PRIMARY KEY (class_id, subject_id)
);

CREATE TABLE student_class_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id),
    class_id UUID NOT NULL REFERENCES classes(id),
    academic_year VARCHAR(20) NOT NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(student_id, class_id, academic_year)
);
