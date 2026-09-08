-- V17: Create administration tables

CREATE TABLE system_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    setting_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    description VARCHAR(500),
    is_public BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_settings_institution FOREIGN KEY (institution_id) REFERENCES institutions(id),
    CONSTRAINT uq_setting_key_institution UNIQUE (institution_id, setting_key)
);

CREATE INDEX idx_settings_institution ON system_settings(institution_id) WHERE is_deleted = false;

CREATE TABLE custom_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    is_system BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_custom_roles_institution FOREIGN KEY (institution_id) REFERENCES institutions(id),
    CONSTRAINT uq_custom_role_code UNIQUE (institution_id, code)
);

CREATE TABLE role_permissions_admin (
    role_id UUID NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, permission_code)
);

CREATE TABLE user_role_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    assigned_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_ura_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ura_role FOREIGN KEY (role_id) REFERENCES custom_roles(id),
    CONSTRAINT fk_ura_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE INDEX idx_ura_user ON user_role_assignments(user_id) WHERE is_deleted = false;

CREATE TABLE data_import_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    import_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_rows INTEGER DEFAULT 0,
    processed_rows INTEGER DEFAULT 0,
    successful_rows INTEGER DEFAULT 0,
    failed_rows INTEGER DEFAULT 0,
    error_log TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_import_jobs_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE TABLE dashboard_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    snapshot_date DATE NOT NULL DEFAULT CURRENT_DATE,
    total_students INTEGER DEFAULT 0,
    total_teachers INTEGER DEFAULT 0,
    total_classes INTEGER DEFAULT 0,
    total_courses INTEGER DEFAULT 0,
    active_enrollments INTEGER DEFAULT 0,
    attendance_rate DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_snapshots_institution FOREIGN KEY (institution_id) REFERENCES institutions(id),
    CONSTRAINT uq_snapshot_date UNIQUE (institution_id, snapshot_date)
);
