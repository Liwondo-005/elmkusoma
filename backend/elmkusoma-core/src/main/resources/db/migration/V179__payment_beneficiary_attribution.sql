-- V179: record who actually earned a payment.
--
-- The money split already exists: payments carries commission_amount,
-- provider_share_amount and the commission policy that produced them. What is
-- missing is the identity on the other end of that split. CommissionService
-- allocates against a service type and an institution; ParentPaymentService
-- called it with providerId = null, so no payment ever recorded *which person*
-- earned the provider share.
--
-- The consequence is that per-persona earnings cannot be derived at all. Only an
-- institution total can be stated, and attributing a share to a teacher, trainer
-- or event organiser afterwards is impossible without guessing.
--
-- So this adds the beneficiary explicitly:
--
--   beneficiary_id     the person or account the provider share belongs to
--   beneficiary_type   which space that id belongs to. Constrained, so a user id
--                      can never be recorded as if it were an organiser id.
--
-- Deliberately NOT backfilled. Existing payments were confirmed allocated with a
-- null provider, so the only truthful value for historical rows is NULL. Inventing
-- a beneficiary from a current profile would fabricate earnings for money that was
-- in fact unattributed, and would silently misstate history that has already been
-- reported on. NULL is kept visible to reconciliation as "unattributed" instead.
--
-- New payments record a beneficiary at creation, where the service being bought is
-- known. Historical transactions stay unattributed and are surfaced, not guessed.

ALTER TABLE payments ADD COLUMN IF NOT EXISTS beneficiary_id uuid;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS beneficiary_type varchar(32);

-- PostgreSQL has no ADD CONSTRAINT IF NOT EXISTS, so drop and recreate. This keeps
-- the migration re-runnable, which the V175 reconciliation path may require.
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_beneficiary_type_ck;
ALTER TABLE payments ADD CONSTRAINT payments_beneficiary_type_ck
    CHECK (beneficiary_type IS NULL
           OR beneficiary_type IN ('USER', 'PROVIDER', 'TEACHER', 'TRAINER', 'ORGANIZER', 'INSTITUTION'));

-- A beneficiary id and its type are set together or not at all. A half-populated
-- row would mean someone earns a share nobody can attribute.
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_beneficiary_pair_ck;
ALTER TABLE payments ADD CONSTRAINT payments_beneficiary_pair_ck
    CHECK ((beneficiary_id IS NULL) = (beneficiary_type IS NULL));

-- Earnings are always read per beneficiary over a date range.
CREATE INDEX IF NOT EXISTS idx_payments_beneficiary
    ON payments (beneficiary_id, paid_at)
    WHERE is_deleted = false AND beneficiary_id IS NOT NULL;