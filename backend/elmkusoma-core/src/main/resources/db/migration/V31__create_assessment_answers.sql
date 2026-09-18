CREATE TABLE IF NOT EXISTS assessment_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES assessment_attempts(id),
    question_id UUID NOT NULL,
    selected_option_id UUID,
    text_answer TEXT,
    is_correct BOOLEAN,
    points_earned DECIMAL(5,2),
    institution_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_assessment_answers_attempt ON assessment_answers(attempt_id);
