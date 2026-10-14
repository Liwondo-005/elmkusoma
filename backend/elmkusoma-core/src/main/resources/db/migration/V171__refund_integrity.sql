-- V171: Refund financial integrity - no payment may ever record more refunded than charged.
--
-- The defect this guards against was real, not theoretical. Two independent paths could move
-- money on one payment:
--
--   * the legacy admin endpoint, which set status = REFUNDED and revoked the entitlement but
--     never recorded the amount in refunded_amount; and
--   * the governed refund lifecycle, which accumulates refunded_amount.
--
-- Because the refundable balance is computed as amount - refunded_amount, a payment refunded
-- through the legacy path still looked fully refundable, so the governed lifecycle would refund
-- it again - the same money returned twice. The application now routes both paths through one
-- adjustment that takes a row lock and re-checks the balance at completion, but a financial
-- invariant this basic should not depend on every call site remembering to do it right. This
-- constraint is the storage-layer backstop: even a future path that skips the check cannot
-- over-refund a payment.
--
-- Additive only. No column is altered, no history is rewritten, and every existing row already
-- satisfies both rules because refunded_amount defaults to 0 and was never written above the
-- charged amount by the governed path.

ALTER TABLE payments
    ADD CONSTRAINT chk_refunded_not_overpaid
    CHECK (refunded_amount <= amount);

ALTER TABLE payments
    ADD CONSTRAINT chk_refunded_not_negative
    CHECK (refunded_amount >= 0);
