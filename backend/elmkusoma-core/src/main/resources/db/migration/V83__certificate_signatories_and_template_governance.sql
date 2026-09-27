-- V82: Certificate governance (ADDITIVE ONLY)
--   1) certificate_signatories            — authorised signatory profiles + authorization scope
--   2) certificate_template_signatories   — template <-> signatory links (ordered, multiple)
--   3) certificate_templates.version      — current template version counter
--   4) certificate_template_versions      — archived content of previous versions (coexistence)

-- ─────────────────────────────────────────────────────────────────────────────
-- 1) Signatories
--    institution_id NULL  => platform-wide signatory
--    institution_id set   => scoped to that institution only
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE certificate_signatories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    full_name VARCHAR(200) NOT NULL,
    position_title VARCHAR(200),
    organization VARCHAR(200),
    signature_image TEXT,                -- data:image/...;base64,... or https:// URL (never a file path)
    certificate_types VARCHAR(200),      -- CSV of COMPLETION,ACHIEVEMENT,PARTICIPATION,TRANSCRIPT; NULL = all types
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | INACTIVE
    valid_from DATE,
    valid_until DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_cert_signatories_institution FOREIGN KEY (institution_id) REFERENCES institutions(id),
    CONSTRAINT chk_cert_signatories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_cert_signatories_institution ON certificate_signatories(institution_id) WHERE is_deleted = false;
CREATE INDEX idx_cert_signatories_status ON certificate_signatories(status) WHERE is_deleted = false;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2) Template <-> signatory links (a template may carry several signatories)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE certificate_template_signatories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    template_id UUID NOT NULL,
    signatory_id UUID NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_cert_tpl_sign_template FOREIGN KEY (template_id) REFERENCES certificate_templates(id),
    CONSTRAINT fk_cert_tpl_sign_signatory FOREIGN KEY (signatory_id) REFERENCES certificate_signatories(id),
    CONSTRAINT uq_cert_tpl_sign UNIQUE (template_id, signatory_id)
);

CREATE INDEX idx_cert_tpl_sign_template ON certificate_template_signatories(template_id) WHERE is_deleted = false;

-- ─────────────────────────────────────────────────────────────────────────────
-- 3) Template version counter (existing rows => version 1)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE certificate_templates ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

-- ─────────────────────────────────────────────────────────────────────────────
-- 4) Archived template versions (previous content preserved on edit)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE certificate_template_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    template_id UUID NOT NULL,
    version INTEGER NOT NULL,
    name VARCHAR(200) NOT NULL,
    template_type VARCHAR(30) NOT NULL,
    description TEXT,
    html_content TEXT,
    css_content TEXT,
    logo_url VARCHAR(500),
    archived_at TIMESTAMP NOT NULL DEFAULT NOW(),
    archived_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_cert_tpl_ver_template FOREIGN KEY (template_id) REFERENCES certificate_templates(id),
    CONSTRAINT uq_cert_tpl_ver UNIQUE (template_id, version)
);

CREATE INDEX idx_cert_tpl_versions_template ON certificate_template_versions(template_id) WHERE is_deleted = false;
