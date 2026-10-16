-- V178: who actually paid for a payment.
--
-- The problem this fixes is not cosmetic. `payments.parent_id` is documented and universally
-- read as "the parent account that pays", and `payments.student_id` as "the learner the access
-- is for". Both are NOT NULL, and both are foreign identifier spaces unrelated to `users.id`.
-- Yet event ticket payments wrote the attendee's *user* id into both columns, because that was
-- the only id the booking request carried.
--
-- The consequences were real rather than theoretical:
--   * payer-scoped reporting grouped a payment under a `parents` row that does not exist;
--   * refunds and the parent inbox could never see money an event had collected;
--   * reconciliation could not attribute an unmatched transaction to anybody.
--
-- Worse, the NOT NULL constraints made the schema unable to describe a legitimate purchase at
-- all: a teacher or staff member buying their own ticket is a paying attendee with no parent and
-- no student record, and there was no way to record that without inventing one.
--
-- So "who paid" is separated from "whose account" and "whose access":
--
--   purchaser_id / purchaser_type  the actor who paid. Always populated going forward.
--                               'USER', 'PARENT' or 'INSTITUTION'. This is the attribution
--                               key reports, refunds and reconciliation group by.
--   parent_id    nullable now. Present when a parent really did pay for a child.
--   student_id   nullable now. Present when the access really is for a learner.
--
-- Additive and forward-only. Nothing is deleted, no payment is rewritten beyond the backfill,
-- and no historical transaction is invented: existing rows are simply recorded as having been
-- paid by a parent, which is the only interpretation the old schema permitted.
--
-- Backfill is therefore exact, not a guess. Every existing row had a non-null parent_id, so
-- 'PARENT' is the only truthful classification available for them. Rows that were in fact
-- written with a user id (the event tickets) are recorded as PARENT rather than as USER,
-- because the migration cannot know which they were; that historical ambiguity is why the
-- reconciliation view exists, and it is left visible rather than papered over.
--
-- No foreign keys are added: `payments` currently has none, and adding them here would be a
-- separate integrity decision that depends on the parent/student history this migration does
-- not attempt to rewrite.

ALTER TABLE payments ALTER COLUMN parent_id DROP NOT NULL;
ALTER TABLE payments ALTER COLUMN student_id DROP NOT NULL;

ALTER TABLE payments ADD COLUMN IF NOT EXISTS purchaser_id uuid;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS purchaser_type varchar(32);

-- Existing payments predate this distinction, and every one of them has a parent_id, so PARENT
-- is the only classification the recorded data supports.
UPDATE payments
   SET purchaser_id = parent_id,
       purchaser_type = 'PARENT'
 WHERE purchaser_id IS NULL
   AND parent_id IS NOT NULL;

-- Payments owned by an institution rather than a person (bulk/corporate enrolment).
ALTER TABLE payments ADD CONSTRAINT payments_purchaser_type_ck
    CHECK (purchaser_type IS NULL OR purchaser_type IN ('USER', 'PARENT', 'INSTITUTION'));

CREATE INDEX IF NOT EXISTS idx_payments_purchaser
    ON payments (purchaser_type, purchaser_id)
    WHERE is_deleted = false;