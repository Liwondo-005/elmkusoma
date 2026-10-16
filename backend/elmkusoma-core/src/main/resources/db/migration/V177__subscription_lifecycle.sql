-- V177: subscription lifecycle records.
--
-- Additive only. Nothing existing is altered, renamed or rewritten.
--
-- Why this table exists at all: `entitlements` answers "may this learner use the
-- service right now", and it must keep answering that alone. It does not answer
-- "what did they pay for, for which period, and has it been cancelled" - and
-- trying to overload it for both questions is what makes renewals re-grant
-- access and cancellations lose their history. This table is the commercial
-- record; access stays where it is.
--
-- Deliberately absent, because each is unresolved business policy rather than
-- implementation detail: billing period length, grace period, cancellation
-- effective date, renewal-failure handling and automatic-renewal consent. The
-- period is carried as start/end taken from the approved access duration already
-- snapshotted on the payment, so this table introduces no second duration rule.
--
-- `payment_id` is UNIQUE, so one paid period opens exactly one subscription and a
-- duplicated gateway callback cannot open a second one. Renewal is a new row
-- rather than a mutation, which keeps the history of what was paid append-only.

CREATE TABLE IF NOT EXISTS subscriptions (
    id                  uuid PRIMARY KEY,
    institution_id      uuid,
    student_id          uuid        NOT NULL,
    payer_id            uuid        NOT NULL,
    service_type        varchar(100) NOT NULL,
    service_id          uuid,
    payment_id          uuid        NOT NULL UNIQUE,
    entitlement_id      uuid,
    status              varchar(32) NOT NULL DEFAULT 'ACTIVE',
    period_start        timestamp   NOT NULL,
    period_end          timestamp   NOT NULL,
    cancelled_at        timestamp,
    cancellation_reason varchar(500),
    created_at          timestamp   NOT NULL,
    updated_at          timestamp,
    -- created_by/updated_by are VARCHAR, not uuid, and not by accident.
    -- BaseEntity maps them as String populated by authentication.getName(); every other
    -- table in this schema declares them the same way. Declaring them as uuid here would
    -- make each audited insert into subscriptions fail with a bare HTTP 500 at runtime,
    -- long after the migration itself had succeeded.
    created_by          varchar(255),
    updated_by          varchar(255),
    is_deleted          boolean     NOT NULL DEFAULT false,
    CONSTRAINT subscriptions_status_ck
        CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED'))
);

-- The lifecycle scan (expire elapsed periods) and "what does this learner hold"
-- both read by student, and the reconciliation sweep reads by status.
CREATE INDEX IF NOT EXISTS idx_subscriptions_student
    ON subscriptions (student_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_subscriptions_status_period_end
    ON subscriptions (status, period_end)
    WHERE is_deleted = false;

-- Adjacent paid periods are allowed; overlapping ones are not.
--
-- Renewal chains: a verified renewal bought while the current period is still live
-- opens a NEW period beginning exactly at the current period's end. Two ACTIVE rows
-- therefore legitimately coexist for one payer and service, so the older "one
-- active row" unique index is both too strict (it rejected every renewal) and too
-- weak (it never actually prevented two rows that overlapped in time).
--
-- The correct guarantee is non-overlap, expressed in the database so it cannot be
-- bypassed by a bug or a concurrent request:
--
--   * COALESCE on service_id is load-bearing. PostgreSQL treats NULLs as distinct in
--     a unique index, so a plain (payer_id, service_type, service_id) key would never
--     fire for exactly the rows that matter most here - access tiers such as
--     PREMIUM_ACCESS carry no service_id, so every such row would be NULL and the
--     constraint would sit idle. Collapsing NULL to a sentinel makes them collide.
--
--   * tsrange(..., '[)') is half-open, so the new period may START where the old one
--     ends without colliding. A closed range '[ ]' would treat a chained renewal as
--     an overlap and reject it again.
--
-- Requires the btree_gist extension for the equality operators alongside the range
-- operator. If CREATE EXTENSION is unavailable to the deployment role, this
-- migration must fail loudly rather than silently degrade - see the fallback note
-- in the Phase C report.
CREATE EXTENSION IF NOT EXISTS btree_gist;

DROP INDEX IF EXISTS uniq_subscriptions_active_service;

ALTER TABLE subscriptions DROP CONSTRAINT IF EXISTS ex_subscriptions_active_no_overlap;

ALTER TABLE subscriptions
    ADD CONSTRAINT ex_subscriptions_active_no_overlap
    EXCLUDE USING gist (
        payer_id WITH =,
        service_type WITH =,
        COALESCE(service_id, '00000000-0000-0000-0000-000000000000'::uuid) WITH =,
        tsrange(period_start, period_end, '[)') WITH &&
    )
    WHERE (is_deleted = false AND status = 'ACTIVE');