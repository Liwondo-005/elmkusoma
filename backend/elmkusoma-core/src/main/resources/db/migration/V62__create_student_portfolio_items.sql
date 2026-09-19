CREATE TABLE student_portfolio_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    file_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    portfolio_type VARCHAR(50) NOT NULL,
    subject_name VARCHAR(100),
    display_order INTEGER DEFAULT 0,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_student_portfolio_items_student ON student_portfolio_items(student_id);
CREATE INDEX idx_student_portfolio_items_institution ON student_portfolio_items(institution_id);
CREATE INDEX idx_student_portfolio_items_type ON student_portfolio_items(portfolio_type);
