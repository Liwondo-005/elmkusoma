-- V16: Create certificate tables

CREATE TABLE certificate_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    template_html TEXT NOT NULL,
    logo_url VARCHAR(500),
    signature_url VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_cert_templates_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_cert_templates_institution ON certificate_templates(institution_id) WHERE is_deleted = false;

CREATE TABLE certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    template_id UUID,
    certificate_number VARCHAR(100) UNIQUE NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    issued_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    verification_code VARCHAR(100) UNIQUE NOT NULL,
    pdf_url VARCHAR(500),
    revoked_at TIMESTAMP,
    revocation_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_certificates_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_certificates_template FOREIGN KEY (template_id) REFERENCES certificate_templates(id),
    CONSTRAINT fk_certificates_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_certificates_student ON certificates(student_id) WHERE is_deleted = false;
CREATE INDEX idx_certificates_number ON certificates(certificate_number) WHERE is_deleted = false;
CREATE INDEX idx_certificates_verification ON certificates(verification_code) WHERE is_deleted = false;

CREATE TABLE transcripts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    student_id UUID NOT NULL,
    transcript_number VARCHAR(100) UNIQUE NOT NULL,
    academic_year_id UUID NOT NULL,
    issued_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    pdf_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_transcripts_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_transcripts_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_transcripts_student ON transcripts(student_id) WHERE is_deleted = false;

CREATE TABLE transcript_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    transcript_id UUID NOT NULL,
    subject_name VARCHAR(200) NOT NULL,
    grade VARCHAR(10),
    marks DECIMAL(5,2),
    credit_hours INTEGER,
    comments TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_transcript_entries_transcript FOREIGN KEY (transcript_id) REFERENCES transcripts(id),
    CONSTRAINT fk_transcript_entries_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_transcript_entries_transcript ON transcript_entries(transcript_id) WHERE is_deleted = false;
