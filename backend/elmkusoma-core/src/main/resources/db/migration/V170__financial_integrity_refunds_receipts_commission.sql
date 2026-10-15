-- V170: Financial integrity, refund lifecycle, receipts, commissions and reconciliation.
--
-- Everything here is additive and non-destructive: no table is dropped, no column is renamed,
-- no transaction history is rewritten. All money columns are DECIMAL(12,2) - never floating
-- point - and every policy-bearing table is versioned so a historical transaction can still be
-- read back against the terms it was actually sold under.
--
-- Nothing in this migration activates a price, a commission rate, a gateway or a payout.
-- Commission and discount policies are seeded with NO rate at all: Payment.md forbids inventing
-- a universal commission percentage, so the rate column is left NULL until an authorized
-- business decision is recorded.

-- ---------------------------------------------------------------------------
-- 1. payments: preserve the terms each transaction was sold under, plus idempotency.
-- ---------------------------------------------------------------------------
ALTER TABLE payments ADD COLUMN IF NOT EXISTS policy_key VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS policy_version INTEGER;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gross_amount DECIMAL(12,2);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS net_amount DECIMAL(12,2);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS access_days INTEGER;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refunded_amount DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(64);
-- The allocation computed at confirmation time, stamped onto the transaction. Reading today's
-- policy and applying it to last month's sale would silently recalculate history, which the
-- rules forbid. NULL means "no approved rate existed", which is not the same as zero.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS commission_amount DECIMAL(12,2);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS provider_share_amount DECIMAL(12,2);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS commission_status VARCHAR(40);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS commission_policy_key VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS commission_policy_version INTEGER;

-- Two concurrent initiation attempts for the same purchase must not create two payments.
-- Scoped per institution so two tenants cannot collide on the same client-generated key.
CREATE UNIQUE INDEX IF NOT EXISTS uniq_payments_idempotency
    ON payments(institution_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL AND is_deleted = FALSE;

-- Exactly one receipt per payment; a refund produces a credit note, never a second receipt.
CREATE UNIQUE INDEX IF NOT EXISTS uniq_payments_receipt_number
    ON payments(receipt_number)
    WHERE receipt_number IS NOT NULL AND is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- 2. commission_policies - configurable, versioned, and deliberately rate-free.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS commission_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    policy_key VARCHAR(100) NOT NULL,
    policy_version INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    service_type VARCHAR(50),
    provider_id UUID,
    -- NULL until an authorized business decision exists. A commission rate is NEVER invented;
    -- an allocation whose applicable policy has no rate is reported as unresolved, not zeroed.
    commission_rate DECIMAL(7,6),
    fixed_amount DECIMAL(12,2),
    -- Explicitly records who bears the gateway processing fee. NULL means "undecided", which is
    -- the honest state; silently assuming either party bears it would invent a business term.
    fee_bearer VARCHAR(30),
    currency VARCHAR(10) DEFAULT 'TZS',
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_commission_active_service_type
    ON commission_policies(service_type)
    WHERE status = 'ACTIVE' AND is_deleted = FALSE AND service_type IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uniq_commission_active_provider
    ON commission_policies(provider_id)
    WHERE status = 'ACTIVE' AND is_deleted = FALSE AND provider_id IS NOT NULL AND service_type IS NULL;
CREATE INDEX IF NOT EXISTS idx_commission_policies_status ON commission_policies(status);
CREATE INDEX IF NOT EXISTS idx_commission_policies_key ON commission_policies(policy_key, policy_version);

-- No commission policy is seeded, and none carries a rate. Until an authorized ACTIVE policy
-- exists, every allocation is reported as UNRESOLVED rather than assuming a percentage.
INSERT INTO commission_policies
    (policy_key, policy_version, status, service_type, commission_rate, description)
SELECT 'PLATFORM_COMMISSION', 1, 'DRAFT', s.service_type, NULL,
       'Unapproved placeholder - no rate set; requires authorized commission decision'
FROM (VALUES
        ('LEARNING_RESOURCE'), ('ONLINE_COURSE'), ('VOCATIONAL_MODULE'), ('LIVE_CLASS'),
        ('RECORDED_LESSON'), ('CERTIFICATE_REPRINT'), ('EVENT_TICKET'),
        ('COMMUNITY_EVENT_TICKET'), ('DAILY_ACCESS'), ('WEEKLY_ACCESS'), ('MONTHLY_ACCESS'),
        ('BASIC_ACCESS'), ('PLUS_ACCESS'), ('PREMIUM_ACCESS')) AS s(service_type)
WHERE NOT EXISTS (
    SELECT 1 FROM commission_policies cp
    WHERE cp.policy_key = 'PLATFORM_COMMISSION' AND cp.service_type = s.service_type
      AND cp.is_deleted = FALSE);

-- ---------------------------------------------------------------------------
-- 3. refund_requests - a request is never a completed refund.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refund_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    payment_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    evidence_url VARCHAR(500),
    -- REQUESTED -> UNDER_REVIEW -> APPROVED -> PROCESSING -> COMPLETED, or REJECTED/CANCELLED.
    -- PROCESSING exists because no live gateway is enabled: an approved refund is NOT a refund
    -- that has actually returned money, and the UI must never say otherwise.
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    decision_note VARCHAR(500),
    decided_by UUID,
    decided_at TIMESTAMP,
    completed_at TIMESTAMP,
    refund_reference VARCHAR(200),
    entitlement_action VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_refund_requests_payment ON refund_requests(payment_id);
CREATE INDEX IF NOT EXISTS idx_refund_requests_status ON refund_requests(status);
CREATE INDEX IF NOT EXISTS idx_refund_requests_institution ON refund_requests(institution_id);

-- Duplicate-refund protection at the storage layer: only one refund may be in flight at a time
-- for a given payment. A second concurrent request is refused by Postgres, not just by app code.
CREATE UNIQUE INDEX IF NOT EXISTS uniq_refund_open_per_payment
    ON refund_requests(payment_id)
    WHERE status IN ('REQUESTED', 'UNDER_REVIEW', 'APPROVED', 'PROCESSING')
      AND is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- 4. payment_receipts - a receipt documents money received, linked to one payment.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    payment_id UUID NOT NULL,
    receipt_number VARCHAR(64) NOT NULL,
    parent_id UUID NOT NULL,
    student_id UUID,
    service_type VARCHAR(50) NOT NULL,
    service_id UUID,
    service_name VARCHAR(200),
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'TZS',
    payment_channel VARCHAR(50),
    provider_reference VARCHAR(200),
    paid_at TIMESTAMP,
    policy_key VARCHAR(100),
    policy_version INTEGER,
    refunded_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    credit_note_number VARCHAR(64),
    credit_note_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_payment_receipts_number
    ON payment_receipts(receipt_number) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uniq_payment_receipts_payment
    ON payment_receipts(payment_id) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_payment_receipts_parent ON payment_receipts(parent_id);
CREATE INDEX IF NOT EXISTS idx_payment_receipts_institution ON payment_receipts(institution_id);

-- ---------------------------------------------------------------------------
-- 5. reconciliation_exceptions - find the mismatches, never rewrite history to balance.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reconciliation_exceptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    -- PENDING_UNRESOLVED | AMOUNT_MISMATCH | DUPLICATE_EXTERNAL_REF | MISSING_ENTITLEMENT |
    -- REFUND_MISMATCH | FEE_MISMATCH | INCONSISTENT_STATUS | MISSING_INTERNAL_RECORD
    exception_type VARCHAR(40) NOT NULL,
    reference VARCHAR(200) NOT NULL,
    payment_id UUID,
    amount DECIMAL(12,2),
    currency VARCHAR(10) DEFAULT 'TZS',
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_to UUID,
    resolution_note VARCHAR(1000),
    resolved_by UUID,
    resolved_at TIMESTAMP,
    discovered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_recon_exceptions_type ON reconciliation_exceptions(exception_type);
CREATE INDEX IF NOT EXISTS idx_recon_exceptions_status ON reconciliation_exceptions(status);
CREATE INDEX IF NOT EXISTS idx_recon_exceptions_institution ON reconciliation_exceptions(institution_id);
CREATE INDEX IF NOT EXISTS idx_recon_exceptions_reference ON reconciliation_exceptions(reference);

-- One open exception per (type, reference): a re-run of detection must not spam duplicates.
CREATE UNIQUE INDEX IF NOT EXISTS uniq_recon_open_per_reference
    ON reconciliation_exceptions(exception_type, reference)
    WHERE status = 'OPEN' AND is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- 6. discount_rules - configurable and versioned; no rule is activated by this migration.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discount_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    policy_key VARCHAR(100) NOT NULL,
    policy_version INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    name VARCHAR(200) NOT NULL,
    service_type VARCHAR(50),
    percent_off DECIMAL(5,2),
    fixed_amount_off DECIMAL(12,2),
    max_uses INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_discount_rules_status ON discount_rules(status);
CREATE INDEX IF NOT EXISTS idx_discount_rules_service ON discount_rules(service_type);
CREATE UNIQUE INDEX IF NOT EXISTS uniq_discount_active_per_service
    ON discount_rules(service_type)
    WHERE status = 'ACTIVE' AND is_deleted = FALSE AND service_type IS NOT NULL;

-- A discount must define exactly one mechanism; a rule with both or neither is ambiguous.
ALTER TABLE discount_rules
    ADD CONSTRAINT chk_discount_one_mechanism
    CHECK (num_nonnulls(percent_off, fixed_amount_off) = 1);
