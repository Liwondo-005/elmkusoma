CREATE TABLE student_course_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    course_id UUID NOT NULL,
    programme_id UUID,
    semester VARCHAR(20),
    academic_year VARCHAR(20),
    credit_hours INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    enrolled_date DATE,
    completed_date DATE,
    grade VARCHAR(5),
    grade_points DOUBLE PRECISION,
    instructor_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_sce_student ON student_course_enrollments(student_id);
CREATE INDEX idx_sce_course ON student_course_enrollments(course_id);
CREATE INDEX idx_sce_programme ON student_course_enrollments(programme_id);
CREATE INDEX idx_sce_status ON student_course_enrollments(status);
CREATE INDEX idx_sce_student_semester ON student_course_enrollments(student_id, semester, academic_year);

CREATE TABLE academic_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    programme_id UUID,
    academic_year VARCHAR(20),
    semester VARCHAR(20),
    total_credit_hours INTEGER,
    earned_credit_hours INTEGER,
    semester_gpa DOUBLE PRECISION,
    cumulative_gpa DOUBLE PRECISION,
    total_courses INTEGER,
    completed_courses INTEGER,
    failed_courses INTEGER,
    academic_standing VARCHAR(30),
    class_rank INTEGER,
    total_students_in_class INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ar_student ON academic_records(student_id);
CREATE INDEX idx_ar_programme ON academic_records(programme_id);
CREATE INDEX idx_ar_student_semester ON academic_records(student_id, semester, academic_year);

CREATE TABLE career_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    career_objective TEXT,
    target_industry VARCHAR(100),
    target_role VARCHAR(150),
    skills TEXT,
    certifications TEXT,
    experience_summary TEXT,
    cv_file_url VARCHAR(500),
    linkedin_url VARCHAR(500),
    portfolio_url VARCHAR(500),
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_cp_student ON career_profiles(student_id);
