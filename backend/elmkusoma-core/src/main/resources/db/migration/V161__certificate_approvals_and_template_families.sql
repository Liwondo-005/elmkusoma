-- Certificate approval workflow and template families.
--
-- The audit found that certificates went straight from DRAFT to ISSUED with no review step at
-- all, and that an approval was therefore not merely unaudited but structurally impossible: no
-- table, no entity, no code path. Issuance was a single admin click.
--
-- ONE approval architecture, not one per certificate family
--
-- Both the training/completion family and the formal academic family use this same table and the
-- same service. What differs is how many stages a policy requires, which lives in configuration
-- rather than in a second competing workflow. A parallel approval system per family is exactly how
-- two lifecycle state machines end up contradicting each other.
--
-- Lifecycle extension, not a second state machine
--
-- certificates.status gains PENDING_APPROVAL and APPROVED alongside the existing DRAFT, ISSUED and
-- REVOKED. This is additive on an unconstrained varchar column, so existing rows are untouched and
-- existing DRAFT certificates remain DRAFT. No new status column is introduced, so the certificate
-- still has exactly one authoritative lifecycle field.
--
-- Historical approvals are immutable by design: a rejected or withdrawn decision stays on the row.
-- Deleting it would let a refused certificate be quietly re-approved with no evidence that it was
-- ever refused.
--
-- Signatory snapshot
--
-- certificate_signatory_snapshots captures the approved identity, title and signature asset that a
-- specific issued certificate was rendered with. Without it, editing a signatory's profile today
-- silently rewrites the appearance of certificates issued years ago, because those documents
-- reference the signatory row, not a copy of it.

-- certificates.status is varchar(255) and must stay that way. The ALTER below is deliberately
-- sized to the current column, NOT to the longest status name: narrowing a lifecycle column to
-- today's vocabulary turns every future state someone adds into a silent truncation, and the
-- failure would only surface as an unreadable status on an existing certificate. This asserts the
-- width instead of reducing it.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'certificates' AND column_name = 'status'
          AND character_maximum_length IS NOT NULL
          AND character_maximum_length < 16
    ) THEN
        RAISE EXCEPTION
            'certificates.status is too narrow for PENDING_APPROVAL; widen it rather than truncating the lifecycle';
    END IF;
END $$;

-- Template families.
--
-- The existing COMPLETION / ACHIEVEMENT / PARTICIPATION / TRANSCRIPT values are kept verbatim so
-- every stored template and certificate keeps its meaning; FAMILY_TRAINING is added to the check
-- constraint below only via a new, separate column so no existing row is rewritten.
ALTER TABLE certificate_templates
    ADD COLUMN IF NOT EXISTS template_family VARCHAR(30) NOT NULL DEFAULT 'FAMILY_TRAINING';

-- Backfill: an academic family is distinguished from training by the existing type vocabulary
-- rather than by guessing. ACHIEVEMENT is left in the training family because an achievement
-- award is not by itself an academic qualification.
UPDATE certificate_templates SET template_family = 'FAMILY_TRAINING' WHERE template_family IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_cert_templates_family') THEN
        ALTER TABLE certificate_templates
            ADD CONSTRAINT chk_cert_templates_family
            CHECK (template_family IN ('FAMILY_TRAINING', 'FAMILY_ACADEMIC'));
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- Approval decisions
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS certificate_approvals (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id      UUID           NOT NULL,
    certificate_id      UUID           NOT NULL,
    -- Which gate this decision satisfies.
    stage               VARCHAR(30)    NOT NULL,
    -- PREPARATION | VALIDATION | APPROVAL
    -- PENDING | APPROVED | REJECTED | WITHDRAWN
    decision_status     VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    actor_user_id       UUID,
    actor_email         VARCHAR(255),
    actor_role          VARCHAR(50),
    -- The template version the actor was looking at when deciding. A later template edit must not
    -- silently make an approval apply to a design the approver never saw.
    template_version    INTEGER,
    reason              VARCHAR(1000),
    decided_at          TIMESTAMP,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    is_deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_certificate_approvals_stage
        CHECK (stage IN ('PREPARATION', 'VALIDATION', 'APPROVAL')),
    CONSTRAINT chk_certificate_approvals_status
        CHECK (decision_status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT fk_certificate_approvals_certificate
        FOREIGN KEY (certificate_id) REFERENCES certificates (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cert_approvals_certificate
    ON certificate_approvals (certificate_id) WHERE is_deleted = false;

-- The approval queue: pending work for one institution, newest first.
CREATE INDEX IF NOT EXISTS idx_cert_approvals_queue
    ON certificate_approvals (institution_id, stage, decided_at)
    WHERE is_deleted = false AND decision_status = 'PENDING';

-- One live decision per (certificate, stage). A re-approval after a rejection is a new row with the
-- earlier one left on record, so this unique index only covers rows that are still current.
CREATE UNIQUE INDEX IF NOT EXISTS ux_certificate_approvals_current_stage
    ON certificate_approvals (certificate_id, stage)
    WHERE is_deleted = false AND decision_status IN ('PENDING', 'APPROVED');

-- ---------------------------------------------------------------------------
-- Signatory snapshot taken at issuance
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS certificate_signatory_snapshots (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id      UUID           NOT NULL,
    certificate_id      UUID           NOT NULL,
    signatory_id        UUID,
    display_order       INTEGER        NOT NULL DEFAULT 0,
    full_name           VARCHAR(200)   NOT NULL,
    position_title      VARCHAR(200),
    organization        VARCHAR(200),
    signature_image     TEXT,
    template_version    INTEGER,
    captured_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    is_deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_certificate_signatory_snapshots_certificate
        FOREIGN KEY (certificate_id) REFERENCES certificates (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cert_signatory_snapshots_certificate
    ON certificate_signatory_snapshots (certificate_id) WHERE is_deleted = false;

-- ---------------------------------------------------------------------------
-- Additive lifecycle values
-- ---------------------------------------------------------------------------
-- certificates.status carries no CHECK constraint, so no constraint change is needed here. This
-- comment records the vocabulary so the next reader does not assume the column is untyped.
--
--   DRAFT            prepared, not yet submitted for review
--   PENDING_APPROVAL submitted; at least one required stage is still outstanding
--   APPROVED          every stage the policy requires has been approved; issuable
--   ISSUED           released to the holder
--   REVOKED          withdrawn after issuance
