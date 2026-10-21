-- V183: persist the platform/provider split of a partial bundle refund (BR-02).
--
-- BundleRefundCalculator has always computed how a refund divides between the platform's
-- recorded commission and the provider's share, but only the refunded total was ever handed to
-- RefundService. The two component figures were therefore discarded at the moment the request was
-- recorded, and reconstructing them later would mean applying today's commission policy to a
-- historical sale - which rewrites what the original split actually was.
--
-- They are stored on the refund request instead, so the split is auditable from the record and
-- matches the policy snapshot on the payment it came from.
--
-- Nullable on purpose: only a bundle partial refund has this split. Refunds raised through any
-- other path have no provider share to divide and stay NULL rather than being recorded as zero,
-- because zero asserts a split that was never calculated.
--
-- Nothing here activates a rate, a gateway, a payout or a refund. No money moves.

ALTER TABLE refund_requests ADD COLUMN IF NOT EXISTS commission_reversed DECIMAL(12,2);
ALTER TABLE refund_requests ADD COLUMN IF NOT EXISTS provider_reversed DECIMAL(12,2);
