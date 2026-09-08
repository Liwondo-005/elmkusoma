-- ELMKUSOMA Core - Certificate Module
-- V13: Create certificate template, certificate, and transcript tables

-- Certificate templates
CREATE TABLE certificate_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    template_type VARCHAR(50) NOT NULL,
    html_content TEXT,
    css_content TEXT,
    logo_url VARCHAR(500),
    signature_line_1 VARCHAR(255),
    signature_line_2 VARCHAR(255),
    signature_line_3 VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_certificate_templates_institution ON certificate_templates(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_certificate_templates_type ON certificate_templates(template_type) WHERE is_deleted = false;

-- Certificates
CREATE TABLE certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    template_id UUID NOT NULL,
    student_id UUID NOT NULL,
    issued_by UUID NOT NULL,
    serial_number VARCHAR(50) UNIQUE NOT NULL,
    certificate_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    student_name VARCHAR(255) NOT NULL,
    student_id_number VARCHAR(100),
    course_or_programme VARCHAR(255),
    completion_date DATE NOT NULL,
    issue_date TIMESTAMP NOT NULL DEFAULT NOW(),
    expiry_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    verification_code VARCHAR(100) UNIQUE NOT NULL,
    verification_url VARCHAR(500),
    qr_code_url VARCHAR(500),
    instructor_name VARCHAR(255),
    grade VARCHAR(50),
    skills JSONB,
    revoked_reason VARCHAR(500),
    revoked_at TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_certificates_institution ON certificates(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_certificates_student ON certificates(student_id) WHERE is_deleted = false;
CREATE INDEX idx_certificates_serial ON certificates(serial_number) WHERE is_deleted = false;
CREATE INDEX idx_certificates_verification ON certificates(verification_code) WHERE is_deleted = false;
CREATE INDEX idx_certificates_status ON certificates(status) WHERE is_deleted = false;
CREATE INDEX idx_certificates_issued_by ON certificates(issued_by) WHERE is_deleted = false;
CREATE INDEX idx_certificates_template ON certificates(template_id) WHERE is_deleted = false;

-- Certificate serial number sequences per institution
CREATE TABLE certificate_sequences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    certificate_type VARCHAR(50) NOT NULL,
    current_number INTEGER NOT NULL DEFAULT 0,
    prefix VARCHAR(20) NOT NULL DEFAULT 'CERT',
    year INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    UNIQUE(institution_id, certificate_type, year)
);

-- Transcripts
CREATE TABLE transcripts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    issued_by UUID NOT NULL,
    serial_number VARCHAR(50) UNIQUE NOT NULL,
    academic_year VARCHAR(50),
    term VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_subjects INTEGER,
    average_score DECIMAL(5,2),
    class_rank INTEGER,
    remarks VARCHAR(500),
    generated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    issued_at TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_transcripts_institution ON transcripts(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_transcripts_student ON transcripts(student_id) WHERE is_deleted = false;
CREATE INDEX idx_transcripts_serial ON transcripts(serial_number) WHERE is_deleted = false;
CREATE INDEX idx_transcripts_status ON transcripts(status) WHERE is_deleted = false;

-- Transcript subject entries
CREATE TABLE transcript_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    transcript_id UUID NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    subject_code VARCHAR(50),
    score DECIMAL(5,2),
    grade VARCHAR(10),
    remarks VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_transcript_entries_transcript ON transcript_entries(transcript_id) WHERE is_deleted = false;
