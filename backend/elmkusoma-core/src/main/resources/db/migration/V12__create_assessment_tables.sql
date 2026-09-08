-- V12: Create assessment tables

CREATE TABLE assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    time_limit_minutes INTEGER,
    total_marks INTEGER NOT NULL DEFAULT 100,
    pass_marks INTEGER NOT NULL DEFAULT 50,
    is_published BOOLEAN NOT NULL DEFAULT false,
    starts_at TIMESTAMP,
    ends_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_assessments_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_assessments_class_group ON assessments(class_group_id) WHERE is_deleted = false;

CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    assessment_id UUID NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    question_text TEXT NOT NULL,
    marks INTEGER NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_questions_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id),
    CONSTRAINT fk_questions_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_questions_assessment ON questions(assessment_id) WHERE is_deleted = false;

CREATE TABLE options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    question_id UUID NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT fk_options_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE TABLE attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    assessment_id UUID NOT NULL,
    student_id UUID NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    submitted_at TIMESTAMP,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_attempts_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id),
    CONSTRAINT fk_attempts_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_attempts_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_attempts_assessment ON attempts(assessment_id) WHERE is_deleted = false;
CREATE INDEX idx_attempts_student ON attempts(student_id) WHERE is_deleted = false;

CREATE TABLE answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    question_id UUID NOT NULL,
    selected_option_id UUID,
    text_answer TEXT,
    is_correct BOOLEAN,
    marks_obtained INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_answers_attempt FOREIGN KEY (attempt_id) REFERENCES attempts(id),
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT fk_answers_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE TABLE assessment_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    assessment_id UUID NOT NULL,
    student_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    total_score INTEGER NOT NULL DEFAULT 0,
    is_passed BOOLEAN NOT NULL DEFAULT false,
    graded_at TIMESTAMP,
    feedback TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_results_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id),
    CONSTRAINT fk_results_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_results_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);
