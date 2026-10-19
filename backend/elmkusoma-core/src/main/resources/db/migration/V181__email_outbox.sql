-- V181: transactional email outbox.
--
-- Why this exists: email events were published straight from the request path, and a
-- broker outage at that moment silently discarded them - the enquiry was stored but
-- the notification was gone, with only a WARN log. Writing the event to this table in
-- the same transaction as the business record makes "the notification exists" a
-- durable fact rather than a hopeful side effect.
--
-- This table does NOT claim the email was sent. Status moves PENDING -> PUBLISHED
-- only once the broker has accepted the message; rendering and SMTP delivery happen
-- later in elmkusoma-workers and can still fail there. Those are deliberately
-- separate facts.
--
-- Deduplication is deliberately NOT keyed on recipient+template. A subscriber who
-- genuinely requests two password resets, or an admin who genuinely answers the same
-- enquiry twice, must produce two distinct events; `idempotency_key` exists so a
-- caller can collapse a *retried* request without collapsing a *repeated* one.
--
-- Additive only: creates a new table, alters nothing, and touches no existing data.

CREATE TABLE IF NOT EXISTS email_outbox (
    id                 uuid PRIMARY KEY,
    institution_id     uuid,
    recipient          varchar(320) NOT NULL,
    subject            varchar(500) NOT NULL,
    template_name      varchar(100) NOT NULL,
    template_variables jsonb,
    -- PENDING (waiting), PUBLISHING (claimed by a relay), PUBLISHED (broker accepted),
    -- FAILED (attempts exhausted - terminal and observable).
    status             varchar(32) NOT NULL DEFAULT 'PENDING',
    attempts           integer     NOT NULL DEFAULT 0,
    max_attempts       integer     NOT NULL DEFAULT 5,
    next_attempt_at    timestamp   NOT NULL,
    -- Lease: a relay claims a row by stamping locked_until. If that relay dies, the
    -- lease simply expires and another relay reclaims it, so a crash cannot strand a
    -- notification permanently.
    locked_until       timestamp,
    locked_by          varchar(100),
    last_error         varchar(500),
    idempotency_key    varchar(200),
    published_at       timestamp,
    -- varchar, not uuid: BaseEntity populates these with authentication.getName(),
    -- and every other table in this schema declares them the same way. As uuid they
    -- would make each audited insert fail at runtime.
    created_by         varchar(255),
    updated_by         varchar(255),
    created_at         timestamp   NOT NULL,
    updated_at         timestamp,
    is_deleted         boolean     NOT NULL DEFAULT false,
    CONSTRAINT email_outbox_status_ck
        CHECK (status IN ('PENDING', 'PUBLISHING', 'PUBLISHED', 'FAILED'))
);

-- The relay's only hot query: "what is due now?". Partial, because published and
-- failed rows are retained for audit and never re-dispatched.
CREATE INDEX IF NOT EXISTS idx_email_outbox_due
    ON email_outbox (next_attempt_at)
    WHERE is_deleted = false AND status IN ('PENDING', 'PUBLISHING');

-- Makes a retried caller request collapse onto the row it already created, without
-- ever collapsing two genuinely separate requests (they carry different keys).
CREATE UNIQUE INDEX IF NOT EXISTS uniq_email_outbox_idempotency
    ON email_outbox (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND is_deleted = false;

-- Operational visibility: "what is terminally broken and since when".
CREATE INDEX IF NOT EXISTS idx_email_outbox_failed
    ON email_outbox (created_at)
    WHERE is_deleted = false AND status = 'FAILED';