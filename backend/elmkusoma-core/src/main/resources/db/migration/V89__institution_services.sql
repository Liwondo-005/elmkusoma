-- V89: Institution service enablement per organization

CREATE TABLE IF NOT EXISTS institution_services (
    id BIGSERIAL PRIMARY KEY,
    institution_id UUID NOT NULL,
    feature_key VARCHAR(60) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    configuration JSONB,
    enabled_at TIMESTAMP,
    enabled_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_institution_service UNIQUE (institution_id, feature_key),
    CONSTRAINT fk_institution_service_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX IF NOT EXISTS idx_institution_services_institution ON institution_services(institution_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_institution_services_feature ON institution_services(feature_key) WHERE is_deleted = false;