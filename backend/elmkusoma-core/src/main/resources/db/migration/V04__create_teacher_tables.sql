-- V4: Create teacher, qualification, and assignment tables

CREATE TABLE teachers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    user_id UUID NOT NULL,
    employee_number VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    specialization VARCHAR(255),
    hire_date DATE,
    bio TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_teachers_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_teachers_institution FOREIGN KEY (institution_id) REFERENCES institutions(id),
    CONSTRAINT uq_teachers_user UNIQUE (user_id, institution_id)
);

CREATE INDEX idx_teachers_institution ON teachers(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_teachers_user ON teachers(user_id) WHERE is_deleted = false;
CREATE INDEX idx_teachers_status ON teachers(status) WHERE is_deleted = false;

CREATE TABLE teacher_qualifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    teacher_id UUID NOT NULL,
    qualification_name VARCHAR(255) NOT NULL,
    institution_name VARCHAR(255) NOT NULL,
    field_of_study VARCHAR(255),
    year_obtained INTEGER,
    certificate_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_qualifications_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    CONSTRAINT fk_qualifications_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_qualifications_teacher ON teacher_qualifications(teacher_id) WHERE is_deleted = false;

CREATE TABLE teacher_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    teacher_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    academic_year VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_assignments_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    CONSTRAINT fk_assignments_institution FOREIGN KEY (institution_id) REFERENCES institutions(id),
    CONSTRAINT uq_teacher_assignment UNIQUE (teacher_id, class_group_id, subject_id, academic_year)
);

CREATE INDEX idx_assignments_teacher ON teacher_assignments(teacher_id) WHERE is_deleted = false;
CREATE INDEX idx_assignments_class_group ON teacher_assignments(class_group_id) WHERE is_deleted = false;
CREATE INDEX idx_assignments_subject ON teacher_assignments(subject_id) WHERE is_deleted = false;
