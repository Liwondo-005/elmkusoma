-- V116: Ward dimension + scheduled regional reports (Regional Administration)
--
-- §23 (Ward geography) and §45 (scheduled/published reports) were the two
-- documented gaps in the Regional Administration command centre. This migration
-- adds both domains additively.
--
-- ── Wards ──────────────────────────────────────────────────────────────────
-- Real administrative wards of the districts that already carry seeded
-- institutions (V47 district list, V58 addresses). Ward-level addresses are NOT
-- stated anywhere in the seed data, so institutions.ward_id stays NULL and the
-- Regional Admin data-quality checks report it as a real finding instead of
-- inventing a link.

CREATE TABLE wards (
    id UUID PRIMARY KEY,
    district_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uk_ward_code UNIQUE (code),
    CONSTRAINT fk_ward_district FOREIGN KEY (district_id) REFERENCES districts(id)
);

CREATE INDEX idx_wards_district ON wards(district_id) WHERE is_deleted = FALSE;

ALTER TABLE institutions ADD COLUMN ward_id UUID;
CREATE INDEX idx_institutions_ward ON institutions(ward_id) WHERE is_deleted = FALSE;

-- Real wards (public Tanzanian administrative geography)
INSERT INTO wards (id, district_id, name, code, is_active, created_at, updated_at, is_deleted) VALUES
('44444444-4444-4444-4444-444444444401', '22222222-2222-2222-2222-222222222201', 'Kariakoo',   'ILA-KAR', TRUE, NOW(), NOW(), FALSE),
('44444444-4444-4444-4444-444444444402', '22222222-2222-2222-2222-222222222201', 'Ilala',      'ILA-ILA', TRUE, NOW(), NOW(), FALSE),
('44444444-4444-4444-4444-444444444403', '22222222-2222-2222-2222-222222222201', 'Mchikichini','ILA-MCH', TRUE, NOW(), NOW(), FALSE),
('44444444-4444-4444-4444-444444444404', '22222222-2222-2222-2222-222222222202', 'Sinza',      'KIN-SIN', TRUE, NOW(), NOW(), FALSE),
('44444444-4444-4444-4444-444444444405', '22222222-2222-2222-2222-222222222202', 'Mikocheni',  'KIN-MIK', TRUE, NOW(), NOW(), FALSE),
('44444444-4444-4444-4444-444444444406', '22222222-2222-2222-2222-222222222202', 'Kawe',       'KIN-KAW', TRUE, NOW(), NOW(), FALSE),
('44444444-4444-4444-4444-444444444407', '22222222-2222-2222-2222-222222222205', 'Sekei',      'ARC-SEK', TRUE, NOW(), NOW(), FALSE),
('44444444-4444-4444-4444-444444444408', '22222222-2222-2222-2222-222222222205', 'Unga',       'ARC-UNG', TRUE, NOW(), NOW(), FALSE),
('44444444-4444-4444-4444-444444444409', '22222222-2222-2222-2222-222222222205', 'Njiro',      'ARC-NJI', TRUE, NOW(), NOW(), FALSE);

-- ── Scheduled reports ──────────────────────────────────────────────────────
-- A regional/district admin schedules a jurisdiction-scoped report; the shared
-- Spring scheduler materialises real figures into run history and notifies the
-- owner. No fake content: the snapshot is computed from the same oversight and
-- governance services the dashboard uses.

CREATE TABLE scheduled_reports (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    region_id UUID,
    district_id UUID,
    report_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    recipients VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    next_run_at TIMESTAMP NOT NULL,
    last_run_at TIMESTAMP,
    run_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_scheduled_report_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_scheduled_report_type CHECK (report_type IN ('PERFORMANCE','ATTENDANCE','LEARNERS','DATA_QUALITY','GOVERNANCE')),
    CONSTRAINT chk_scheduled_report_frequency CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY')),
    CONSTRAINT chk_scheduled_report_status CHECK (status IN ('ACTIVE','PAUSED'))
);

CREATE INDEX idx_scheduled_reports_user ON scheduled_reports(user_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_scheduled_reports_due ON scheduled_reports(status, next_run_at) WHERE is_deleted = FALSE;

CREATE TABLE scheduled_report_runs (
    id UUID PRIMARY KEY,
    scheduled_report_id UUID NOT NULL,
    run_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    summary TEXT,
    error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_scheduled_report_run FOREIGN KEY (scheduled_report_id) REFERENCES scheduled_reports(id) ON DELETE CASCADE,
    CONSTRAINT chk_scheduled_report_run_status CHECK (status IN ('SUCCESS','FAILED'))
);

CREATE INDEX idx_scheduled_report_runs_report ON scheduled_report_runs(scheduled_report_id, run_at DESC);
