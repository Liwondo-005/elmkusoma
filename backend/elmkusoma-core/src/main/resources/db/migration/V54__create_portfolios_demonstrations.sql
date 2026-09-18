CREATE TABLE portfolios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_portfolios_institution ON portfolios(institution_id);
CREATE INDEX idx_portfolios_student ON portfolios(student_id);
CREATE INDEX idx_portfolios_visibility ON portfolios(visibility);

CREATE TABLE portfolio_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    portfolio_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    item_type VARCHAR(30) NOT NULL,
    file_url VARCHAR(500),
    competency_id UUID,
    project_id UUID,
    date_obtained DATE,
    sort_order INTEGER DEFAULT 0,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_portfolio_items_portfolio ON portfolio_items(portfolio_id);
CREATE INDEX idx_portfolio_items_item_type ON portfolio_items(item_type);
CREATE INDEX idx_portfolio_items_competency ON portfolio_items(competency_id);
CREATE INDEX idx_portfolio_items_project ON portfolio_items(project_id);

CREATE TABLE practical_demonstrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    competency_id UUID,
    project_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    media_urls TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    reviewer_id UUID,
    review_notes TEXT,
    score INTEGER,
    reviewed_at TIMESTAMP,
    submitted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_practical_demonstrations_institution ON practical_demonstrations(institution_id);
CREATE INDEX idx_practical_demonstrations_student ON practical_demonstrations(student_id);
CREATE INDEX idx_practical_demonstrations_status ON practical_demonstrations(status);
CREATE INDEX idx_practical_demonstrations_competency ON practical_demonstrations(competency_id);
CREATE INDEX idx_practical_demonstrations_project ON practical_demonstrations(project_id);
CREATE INDEX idx_practical_demonstrations_reviewer ON practical_demonstrations(reviewer_id);
