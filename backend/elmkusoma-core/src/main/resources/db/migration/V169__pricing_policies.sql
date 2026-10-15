-- V169: Pricing policy - versioned, approval-gated platform price catalogue.
--
-- Prices become configuration rather than code. Without a table like this the only place a
-- chargeable amount can live is the client request, which is exactly the defect V169 fixes:
-- ParentSelfController was passing req.getAmount() straight through, so any caller could name
-- its own price. The gateway webhook's amount reconciliation does not defend against that,
-- because it only compares the gateway figure against the already-tainted stored amount.
--
-- Every row is seeded DRAFT. Activation requires an authorized Platform Admin approval, so this
-- migration installs the user-selected TZS catalogue without activating a single price. Until a
-- policy is activated, paid initiation is refused rather than falling back to a client amount.

CREATE TABLE pricing_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    policy_key VARCHAR(100) NOT NULL,
    policy_version INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    service_type VARCHAR(50),
    education_level VARCHAR(30),
    min_amount DECIMAL(12,2),
    max_amount DECIMAL(12,2),
    default_amount DECIMAL(12,2),
    currency VARCHAR(10) DEFAULT 'TZS',
    access_days INTEGER,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_pricing_policies_status ON pricing_policies(status);
CREATE INDEX idx_pricing_policies_key ON pricing_policies(policy_key, policy_version);
CREATE INDEX idx_pricing_policies_service_type ON pricing_policies(service_type);
CREATE INDEX idx_pricing_policies_education_level ON pricing_policies(education_level);

-- A service type may carry at most one ACTIVE price, and a level at most one ACTIVE band.
-- Partial unique indexes make a second activation impossible at the storage layer, not just in
-- application code, so a racing approval cannot end up with two authoritative prices.
CREATE UNIQUE INDEX uniq_pricing_active_service_type
    ON pricing_policies(service_type)
    WHERE status = 'ACTIVE' AND is_deleted = FALSE AND service_type IS NOT NULL;
CREATE UNIQUE INDEX uniq_pricing_active_education_level
    ON pricing_policies(education_level)
    WHERE status = 'ACTIVE' AND is_deleted = FALSE AND education_level IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Education-level bands. Provider catalogue prices must fall inside these.
-- ---------------------------------------------------------------------------
INSERT INTO pricing_policies
    (policy_key, policy_version, status, education_level, min_amount, max_amount, currency, description)
VALUES
    ('EDUCATION_LEVEL_BAND', 1, 'DRAFT', 'PRIMARY',   500, 3000,  'TZS', 'Primary education pricing band'),
    ('EDUCATION_LEVEL_BAND', 1, 'DRAFT', 'SECONDARY', 1000, 5000, 'TZS', 'Secondary education pricing band'),
    ('EDUCATION_LEVEL_BAND', 1, 'DRAFT', 'HIGHER',    1000, 15000,'TZS', 'Higher education, college, university and vocational band');

-- ---------------------------------------------------------------------------
-- Access passes - rolling durations.
-- ---------------------------------------------------------------------------
INSERT INTO pricing_policies
    (policy_key, policy_version, status, service_type, default_amount, currency, access_days, description)
VALUES
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'DAILY_ACCESS',    500,  'TZS', 1,  'Daily access, rolling 24 hours'),
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'WEEKLY_ACCESS',   1000, 'TZS', 7,  'Weekly access, rolling 7 days'),
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'MONTHLY_ACCESS',  5000, 'TZS', 30, 'Monthly access, rolling 30 days');

-- ---------------------------------------------------------------------------
-- Subscription tiers - fixed 30 days, platform-wide.
-- ---------------------------------------------------------------------------
INSERT INTO pricing_policies
    (policy_key, policy_version, status, service_type, default_amount, currency, access_days, description)
VALUES
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'BASIC_ACCESS',  3000, 'TZS', 30, 'Basic subscription tier, 30 days'),
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'PLUS_ACCESS',   5000, 'TZS', 30, 'Plus subscription tier, 30 days'),
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'PREMIUM_ACCESS',7000, 'TZS', 30, 'Premium subscription tier, 30 days');

-- ---------------------------------------------------------------------------
-- Provider catalogue services.
-- ---------------------------------------------------------------------------
INSERT INTO pricing_policies
    (policy_key, policy_version, status, service_type, default_amount, currency, access_days, description)
VALUES
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'LEARNING_RESOURCE',      500,  'TZS', 30, 'Learning resources, notes and revision materials'),
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'ONLINE_COURSE',          2000, 'TZS', 30, 'Standard online course'),
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'VOCATIONAL_MODULE',      1000, 'TZS', 30, 'Academic or vocational module'),
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'LIVE_CLASS',             500,  'TZS', 30, 'Live online class or session'),
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'RECORDED_LESSON',        1000, 'TZS', 30, 'Recorded lesson or replay'),
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'CERTIFICATE_REPRINT',    5000, 'TZS', 30, 'Eligible certificate reprint or duplicate'),
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'EVENT_TICKET',           2000, 'TZS', 30, 'Seminar, workshop or conference ticket'),
    ('SERVICE_DEFAULT_PRICE', 1, 'DRAFT', 'COMMUNITY_EVENT_TICKET', 200,  'TZS', 30, 'Paid community, NGO, CBO, religious or corporate event ticket');
